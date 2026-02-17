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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private MailFolderRepository folderRepository;

    @Autowired
    private MailGroupRepository groupRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailMapper mailMapper;

    // Folder types
    public static final String FOLDER_INBOX = "INBOX";
    public static final String FOLDER_SENT = "SENT";
    public static final String FOLDER_DRAFTS = "DRAFTS";
    public static final String FOLDER_STARRED = "STARRED";
    public static final String FOLDER_ARCHIVE = "ARCHIVE";
    public static final String FOLDER_TRASH = "TRASH";
    public static final String FOLDER_OFFICE = "OFFICE";
    public static final String FOLDER_PERSONAL = "PERSONAL";
    public static final String FOLDER_FREELANCE = "FREELANCE";
    public static final String FOLDER_SHARED = "SHARED";
    public static final String FOLDER_CUSTOM = "CUSTOM";

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

    @Transactional
    public void addMailToFolder(Long mailId, String folderType, User user) {
        log.info("Adding mail {} to folder {} for user {}", mailId, folderType, user.getEmail());

        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        // Check if user has access to this mail
        boolean hasAccess = mail.getSender().getId().equals(user.getId()) ||
                mail.getRecipients().stream().anyMatch(r -> r.getUser() != null && r.getUser().getId().equals(user.getId()));

        if (!hasAccess) {
            throw new RuntimeException("Access denied");
        }

        // Check if already in folder
        boolean alreadyInFolder = folderRepository.isMailInFolder(mailId, user.getId(), folderType);
        if (alreadyInFolder) {
            return; // Already there
        }

        // Create folder association
        MailFolder folder = new MailFolder();
        folder.setUser(user);
        folder.setMail(mail);
        folder.setName(getFolderNameFromType(folderType));
        folder.setFolderType(folderType);
        folder.setColor(getFolderColorFromType(folderType));
        folder.setIsSystem(true);

        folderRepository.save(folder);
    }

    @Transactional
    public void removeMailFromFolder(Long mailId, String folderType, User user) {
        log.info("Removing mail {} from folder {} for user {}", mailId, folderType, user.getEmail());
        folderRepository.removeMailFromFolder(mailId, user.getId(), folderType);
    }

    @Transactional
    public void moveMailToFolder(Long mailId, String fromFolder, String toFolder, User user) {
        if (fromFolder != null && !fromFolder.isEmpty()) {
            removeMailFromFolder(mailId, fromFolder, user);
        }
        addMailToFolder(mailId, toFolder, user);
    }

    public Page<MailResponse> getMailsByFolder(String folderType, User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Mail> mails = folderRepository.findMailsByFolderType(folderType, user, pageable);
        return mails.map(mail -> mailMapper.toResponse(mail, user.getEmail()));
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

    public List<MailFolderResponse> getUserFoldersWithCounts(User user) {
        // Initialize folders if needed
        initializeDefaultFolders(user);

        List<MailFolder> folders = folderRepository.findAllByUserOrdered(user);
        Map<String, Long> folderCounts = getFolderCounts(user);
        Map<String, Long> unreadCounts = getUnreadCounts(user);

        List<MailFolderResponse> responses = new ArrayList<>();

        // Add the default folder types in order
        addFolderResponse(responses, "Inbox", FOLDER_INBOX, "#3B82F6", folderCounts, unreadCounts);
        addFolderResponse(responses, "Office", FOLDER_OFFICE, "#3B82F6", folderCounts, unreadCounts);
        addFolderResponse(responses, "Personal", FOLDER_PERSONAL, "#10B981", folderCounts, unreadCounts);
        addFolderResponse(responses, "Freelance", FOLDER_FREELANCE, "#8B5CF6", folderCounts, unreadCounts);
        addFolderResponse(responses, "Shared", FOLDER_SHARED, "#F59E0B", folderCounts, unreadCounts);

        // Add custom folders
        List<MailFolder> customFolders = folders.stream()
                .filter(f -> FOLDER_CUSTOM.equals(f.getFolderType()))
                .collect(Collectors.toList());

        for (MailFolder folder : customFolders) {
            MailFolderResponse response = new MailFolderResponse();
            response.setId(folder.getId());
            response.setName(folder.getName());
            response.setColor(folder.getColor());
            response.setFolderType(folder.getFolderType());
            response.setIsSystem(folder.getIsSystem());
            response.setMailCount(0);
            response.setUnreadCount(0);
            responses.add(response);
        }

        return responses;
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

    private void addFolderResponse(List<MailFolderResponse> responses, String name, String type, String color,
                                   Map<String, Long> counts, Map<String, Long> unreadCounts) {
        MailFolderResponse response = new MailFolderResponse();
        response.setName(name);
        response.setFolderType(type);
        response.setColor(color);
        response.setIsSystem(true);
        response.setMailCount(counts.getOrDefault(type, 0L).intValue());
        response.setUnreadCount(unreadCounts.getOrDefault(type, 0L).intValue());
        responses.add(response);
    }

    private Map<String, Long> getFolderCounts(User user) {
        Map<String, Long> counts = new HashMap<>();
        counts.put(FOLDER_INBOX, mailRepository.countInboxByUser(user));
        counts.put(FOLDER_OFFICE, folderRepository.countByFolderTypeAndUser(FOLDER_OFFICE, user));
        counts.put(FOLDER_PERSONAL, folderRepository.countByFolderTypeAndUser(FOLDER_PERSONAL, user));
        counts.put(FOLDER_FREELANCE, folderRepository.countByFolderTypeAndUser(FOLDER_FREELANCE, user));
        counts.put(FOLDER_SHARED, folderRepository.countByFolderTypeAndUser(FOLDER_SHARED, user));
        return counts;
    }

    private Map<String, Long> getUnreadCounts(User user) {
        Map<String, Long> counts = new HashMap<>();
        counts.put(FOLDER_INBOX, mailRepository.countUnreadByUser(user));
        counts.put(FOLDER_OFFICE, folderRepository.countUnreadByFolderTypeAndUser(FOLDER_OFFICE, user));
        counts.put(FOLDER_PERSONAL, folderRepository.countUnreadByFolderTypeAndUser(FOLDER_PERSONAL, user));
        counts.put(FOLDER_FREELANCE, folderRepository.countUnreadByFolderTypeAndUser(FOLDER_FREELANCE, user));
        counts.put(FOLDER_SHARED, folderRepository.countUnreadByFolderTypeAndUser(FOLDER_SHARED, user));
        return counts;
    }

    private String getFolderNameFromType(String folderType) {
        switch (folderType) {
            case FOLDER_OFFICE: return "Office";
            case FOLDER_PERSONAL: return "Personal";
            case FOLDER_FREELANCE: return "Freelance";
            case FOLDER_SHARED: return "Shared";
            case FOLDER_INBOX: return "Inbox";
            default: return "Folder";
        }
    }

    private String getFolderColorFromType(String folderType) {
        switch (folderType) {
            case FOLDER_OFFICE: return "#3B82F6";
            case FOLDER_PERSONAL: return "#10B981";
            case FOLDER_FREELANCE: return "#8B5CF6";
            case FOLDER_SHARED: return "#F59E0B";
            case FOLDER_INBOX: return "#3B82F6";
            default: return "#6B7280";
        }
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