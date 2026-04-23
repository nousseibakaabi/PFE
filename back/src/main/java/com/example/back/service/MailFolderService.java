package com.example.back.service;

import com.example.back.entity.Mail;
import com.example.back.entity.MailFolder;
import com.example.back.entity.User;
import com.example.back.entity.MailGroup;
import com.example.back.payload.response.MailFolderResponse;
import com.example.back.payload.response.MailResponse;
import com.example.back.payload.response.MailGroupResponse;
import com.example.back.repository.MailFolderRepository;
import com.example.back.repository.MailGroupRepository;
import com.example.back.repository.MailRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.mapper.MailMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MailFolderService {

    private final MailFolderRepository folderRepository;

    private final MailGroupRepository groupRepository;

    private final MailRepository mailRepository;


    private final MailMapper mailMapper;

    // Folder types
    public static final String FOLDER_INBOX = "INBOX";

    public static final String FOLDER_OFFICE = "OFFICE";
    public static final String FOLDER_PERSONAL = "PERSONAL";
    public static final String FOLDER_FREELANCE = "FREELANCE";
    public static final String FOLDER_SHARED = "SHARED";
    public static final String FOLDER_CUSTOM = "CUSTOM";

    public MailFolderService(MailFolderRepository folderRepository, MailGroupRepository groupRepository, MailRepository mailRepository, MailMapper mailMapper) {
        this.folderRepository = folderRepository;
        this.groupRepository = groupRepository;
        this.mailRepository = mailRepository;
        this.mailMapper = mailMapper;
    }

    @Transactional
    public void initializeDefaultFolders(User user) {
        log.info("Initializing default folders for user: {}", user.getEmail());

        // Check if folders already exist
        List<MailFolder> existingFolders = folderRepository.findByUserAndFolderType(user, FOLDER_OFFICE);
        if (!existingFolders.isEmpty()) {
            return; // Already initialized
        }

        // Create Office folder
        MailFolder office = new MailFolder();
        office.setUser(user);
        office.setName("Office");
        office.setColor("#3B82F6"); // Blue
        office.setFolderType(FOLDER_OFFICE);
        office.setIsSystem(true);
        office.setOrderIndex(1);
        folderRepository.save(office);

        // Create Personal folder
        MailFolder personal = new MailFolder();
        personal.setUser(user);
        personal.setName("Personal");
        personal.setColor("#10B981"); // Green
        personal.setFolderType(FOLDER_PERSONAL);
        personal.setIsSystem(true);
        personal.setOrderIndex(2);
        folderRepository.save(personal);

        // Create Freelance folder
        MailFolder freelance = new MailFolder();
        freelance.setUser(user);
        freelance.setName("Freelance");
        freelance.setColor("#8B5CF6"); // Purple
        freelance.setFolderType(FOLDER_FREELANCE);
        freelance.setIsSystem(true);
        freelance.setOrderIndex(3);
        folderRepository.save(freelance);

        // Create Shared folder
        MailFolder shared = new MailFolder();
        shared.setUser(user);
        shared.setName("Shared");
        shared.setColor("#F59E0B"); // Yellow/Orange
        shared.setFolderType(FOLDER_SHARED);
        shared.setIsSystem(true);
        shared.setOrderIndex(4);
        folderRepository.save(shared);

        log.info("Default folders created for user: {}", user.getEmail());
    }



    public Page<MailResponse> getMailsForGroup(Long groupId, User user, int page, int size) {
        MailGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if user is member of the group
        boolean isMember = group.getIsSystem() ||
                group.getMembers().contains(user) ||
                (group.getOwner() != null && group.getOwner().equals(user));

        if (!isMember) {
            throw new RuntimeException("You are not a member of this group");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());

        // Find mails where the group was specifically addressed (as a recipient)
        // This looks for mails with a recipient that has the group identifier
        String groupIdentifier = "GROUP:" + group.getName();
        Page<Mail> mails = mailRepository.findMailsByGroupRecipient(groupIdentifier, pageable);

        return mails.map(mail -> mailMapper.toResponse(mail, user.getEmail()));
    }


    public List<MailGroupResponse> getGroupsWithUnreadCounts(User user) {
        List<MailGroup> groups = groupRepository.findGroupsForUserWithAccess(user);

        List<MailGroupResponse> responses = new ArrayList<>();
        for (MailGroup group : groups) {
            MailGroupResponse response = mapGroupToResponse(group);

            String groupIdentifier = "GROUP:" + group.getName();

            // Count total mails sent to this group
            long totalCount = mailRepository.countMailsByGroupRecipient(groupIdentifier);
            response.setMailCount(totalCount);

            // Count unread mails for this user in this group
            long unreadCount = mailRepository.countUnreadMailsByGroupForUser(groupIdentifier, user);
            response.setUnreadCount(unreadCount);

            responses.add(response);
        }

        return responses;
    }





    private MailGroupResponse mapGroupToResponse(MailGroup group) {
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
}