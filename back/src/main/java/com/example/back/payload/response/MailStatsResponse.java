package com.example.back.payload.response;

import lombok.Data;

import java.util.List;

@Data
public class MailStatsResponse {
    private long inboxCount;
    private long unreadCount;
    private long sentCount;
    private long draftCount;
    private long starredCount;
    private long archivedCount;
    private long trashCount;

    // Group statistics
    private long systemGroupsCount;
    private long customGroupsCount;
    private long totalGroupsCount;

    private long groupMailsCount;
    private long unreadGroupMailsCount;

    private List<GroupMailStats> groupStats;

    @Data
    public static class GroupMailStats {
        private Long groupId;
        private String groupName;
        private boolean isSystem;
        private long totalMails;
        private long unreadMails;
        private long membersCount;
    }
}