package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.MailGroupRequest;
import com.example.back.payload.response.EmailSuggestionResponse;
import com.example.back.payload.response.MailGroupResponse;
import com.example.back.repository.MailGroupRepository;
import com.example.back.repository.MailRepository;
import com.example.back.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.back.payload.response.MailStatsResponse.GroupMailStats;


@Service
@Slf4j
public class MailGroupService {

    @Autowired
    private MailGroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailRepository mailRepository;



    @Transactional
    public void initializeDefaultGroups() {
        log.info("Initializing default mail groups...");

        // Check if there are any users first
        if (userRepository.count() == 0) {
            log.info("No users found yet, skipping default group initialization");
            return;
        }

        // Create default groups for each role if they don't exist
        Object[][] roleGroups = {
                {"CHEF DE PROJET", ERole.ROLE_CHEF_PROJET},
                {"COMMERCIAL METIER", ERole.ROLE_COMMERCIAL_METIER},
                {"DECIDEUR", ERole.ROLE_DECIDEUR},
                {"ADMIN", ERole.ROLE_ADMIN}
        };

        for (Object[] group : roleGroups) {
            String groupName = (String) group[0];
            ERole roleName = (ERole) group[1];

            if (groupRepository.findByNameAndIsSystemTrue(groupName).isEmpty()) {
                MailGroup defaultGroup = new MailGroup();
                defaultGroup.setName(groupName);
                defaultGroup.setIsSystem(true);
                defaultGroup.setDescription("Auto-generated group for " + roleName);

                // Find admin user as temporary owner (or first user)
                User systemOwner = userRepository.findByUsername("admin")
                        .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

                if (systemOwner != null) {
                    defaultGroup.setOwner(systemOwner);

                    // Add all users with this role
                    List<User> usersWithRole = findUsersByRole(roleName);
                    defaultGroup.setMembers(usersWithRole);

                    groupRepository.save(defaultGroup);
                    log.info("Created default group: {} with {} members", groupName, usersWithRole.size());
                }
            } else {
                log.info("Group {} already exists", groupName);
            }
        }
    }

    @Transactional
    public MailGroupResponse createGroup(MailGroupRequest request, User owner) {
        MailGroup group = new MailGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setOwner(owner);
        group.setIsSystem(false);

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<User> members = userRepository.findAllById(request.getMemberIds());
            group.setMembers(members);
        }

        MailGroup savedGroup = groupRepository.save(group);
        return mapToResponse(savedGroup);
    }

    @Transactional
    public MailGroupResponse updateGroup(Long id, MailGroupRequest request, User user) {
        MailGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if user is owner (can't edit system groups)
        if (!group.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to edit this group");
        }

        if (group.getIsSystem()) {
            throw new RuntimeException("Cannot edit system groups");
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());

        if (request.getMemberIds() != null) {
            List<User> members = userRepository.findAllById(request.getMemberIds());
            group.setMembers(members);
        }

        MailGroup updatedGroup = groupRepository.save(group);
        return mapToResponse(updatedGroup);
    }

    @Transactional
    public void deleteGroup(Long id, User user) {
        MailGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (group.getIsSystem()) {
            throw new RuntimeException("Cannot delete system groups");
        }

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to delete this group");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public MailGroupResponse addMember(Long groupId, Long memberId, User user) {
        MailGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to modify this group");
        }

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!group.getMembers().contains(member)) {
            group.getMembers().add(member);
            group = groupRepository.save(group);
        }

        return mapToResponse(group);
    }

    @Transactional
    public MailGroupResponse removeMember(Long groupId, Long memberId, User user) {
        MailGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to modify this group");
        }

        group.getMembers().removeIf(m -> m.getId().equals(memberId));
        group = groupRepository.save(group);

        return mapToResponse(group);
    }

    // FIXED: This now returns ALL system groups + groups the user is member of
    public List<MailGroupResponse> getUserGroups(User user) {
        // Get all system groups
        List<MailGroup> systemGroups = groupRepository.findByIsSystemTrue();

        // Get groups where user is owner or member
        List<MailGroup> userGroups = groupRepository.findGroupsForUser(user);

        // Combine both lists (system groups + user's groups)
        List<MailGroup> allGroups = new ArrayList<>();
        allGroups.addAll(systemGroups);

        // Add user groups that aren't already in system groups (avoid duplicates)
        for (MailGroup group : userGroups) {
            if (!group.getIsSystem() && !allGroups.contains(group)) {
                allGroups.add(group);
            }
        }

        return allGroups.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MailGroupResponse getGroup(Long id, User user) {
        MailGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if user has access to this group (system groups are accessible to all)
        boolean hasAccess = group.getIsSystem() ||
                group.getOwner().getId().equals(user.getId()) ||
                group.getMembers().contains(user);

        if (!hasAccess) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(group);
    }

    public List<EmailSuggestionResponse> searchEmailsAndGroups(String query, User user) {
        List<EmailSuggestionResponse> suggestions = new ArrayList<>();

        // Search users by email or name
        String searchTerm = "%" + query.toLowerCase() + "%";
        List<User> users = userRepository.searchUsers(searchTerm);

        for (User u : users) {
            if (!u.getId().equals(user.getId())) { // Don't suggest current user
                EmailSuggestionResponse suggestion = new EmailSuggestionResponse();
                suggestion.setEmail(u.getEmail());
                suggestion.setName(u.getFirstName() + " " + u.getLastName());
                suggestion.setType("USER");
                suggestion.setId(u.getId());
                suggestion.setRole(u.getRoles().stream()
                        .findFirst()
                        .map(r -> r.getName().toString())
                        .orElse(""));
                suggestions.add(suggestion);
            }
        }

        // Search ALL system groups and groups user has access to
        List<MailGroup> systemGroups = groupRepository.findByIsSystemTrue();
        List<MailGroup> userGroups = groupRepository.findGroupsForUser(user);

        // Combine all groups for search
        List<MailGroup> allGroups = new ArrayList<>(systemGroups);
        for (MailGroup group : userGroups) {
            if (!group.getIsSystem() && !allGroups.contains(group)) {
                allGroups.add(group);
            }
        }

        for (MailGroup group : allGroups) {
            if (group.getName().toLowerCase().contains(query.toLowerCase())) {
                EmailSuggestionResponse suggestion = new EmailSuggestionResponse();
                suggestion.setEmail("GROUP:" + group.getName());
                suggestion.setName(group.getName());
                suggestion.setType("GROUP");
                suggestion.setId(group.getId());
                suggestion.setGroupName(group.getName());
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    public List<String> getEmailsFromGroup(Long groupId) {
        MailGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return group.getMembers().stream()
                .map(User::getEmail)
                .collect(Collectors.toList());
    }

    private MailGroupResponse mapToResponse(MailGroup group) {
        MailGroupResponse response = new MailGroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setIsSystem(group.getIsSystem());

        if (group.getOwner() != null) {
            response.setOwnerId(group.getOwner().getId());
            response.setOwnerName(group.getOwner().getFirstName() + " " + group.getOwner().getLastName());
        }

        response.setCreatedAt(group.getCreatedAt());
        response.setUpdatedAt(group.getUpdatedAt());

        List<MailGroupResponse.MemberInfo> memberInfos = group.getMembers().stream()
                .map(m -> {
                    MailGroupResponse.MemberInfo info = new MailGroupResponse.MemberInfo();
                    info.setId(m.getId());
                    info.setEmail(m.getEmail());
                    info.setName(m.getFirstName() + " " + m.getLastName());
                    info.setRole(m.getRoles().stream()
                            .findFirst()
                            .map(r -> r.getName().toString())
                            .orElse(""));
                    return info;
                })
                .collect(Collectors.toList());

        response.setMembers(memberInfos);
        return response;
    }

    private List<User> findUsersByRole(ERole roleName) {
        return groupRepository.findUsersByRole(roleName);
    }


    public List<GroupMailStats> getGroupMailStats(User user) {
        List<MailGroup> groups = findGroupsForUserWithAccess(user);
        List<GroupMailStats> stats = new ArrayList<>();

        for (MailGroup group : groups) {
            GroupMailStats groupStat = new GroupMailStats();
            groupStat.setGroupId(group.getId());
            groupStat.setGroupName(group.getName());
            groupStat.setSystem(group.getIsSystem());
            groupStat.setMembersCount(group.getMembers().size());

            String groupIdentifier = "GROUP:" + group.getName();

            // Count total mails sent to this group
            long totalMails = mailRepository.countMailsByGroupRecipient(groupIdentifier);
            groupStat.setTotalMails(totalMails);

            // Count unread mails for this user in this group
            long unreadMails = mailRepository.countUnreadMailsByGroupForUser(groupIdentifier, user);
            groupStat.setUnreadMails(unreadMails);

            stats.add(groupStat);
        }

        return stats;
    }


    public List<MailGroup> findGroupsForUserWithAccess(User user) {
        return groupRepository.findGroupsForUserWithAccess(user);
    }


}