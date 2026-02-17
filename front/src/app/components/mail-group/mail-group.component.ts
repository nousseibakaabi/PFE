import { Component, OnInit } from '@angular/core';
import { MailService, MailGroup, GroupMember, GroupRequest } from '../../services/mail.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-mail-group',
  templateUrl: './mail-group.component.html',
  styleUrls: ['./mail-group.component.css']
})
export class MailGroupComponent implements OnInit {
  systemGroups: MailGroup[] = [];
  customGroups: MailGroup[] = [];
  availableUsers: any[] = [];
  selectedGroup: MailGroup | null = null;
  
  showCreateModal = false;
  showEditModal = false;
  showMembersModal = false;
  
  groupForm: { name: string; description: string } = { name: '', description: '' };
  selectedMemberIds: Set<number> = new Set();
  editingGroup: MailGroup | null = null;
  
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private mailService: MailService,
    private userService: UserService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadGroups();
    this.loadUsers();
  }

  loadGroups(): void {
    this.loading = true;
    this.mailService.getGroups().subscribe({
      next: (response) => {
        if (response.success) {
          const groups = response.data;
          this.systemGroups = groups.filter((g: MailGroup) => g.isSystem);
          this.customGroups = groups.filter((g: MailGroup) => !g.isSystem);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading groups:', error);
        this.errorMessage = 'Failed to load groups';
        this.loading = false;
      }
    });
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (response) => {
        if (response.success) {
          this.availableUsers = response.data;
        }
      },
      error: (error) => console.error('Error loading users:', error)
    });
  }

  openCreateModal(): void {
    this.groupForm = { name: '', description: '' };
    this.selectedMemberIds.clear();
    this.showCreateModal = true;
  }

  openEditModal(group: MailGroup): void {
    this.editingGroup = group;
    this.groupForm = { 
      name: group.name, 
      description: group.description || '' 
    };
    this.selectedMemberIds = new Set(group.members.map(m => m.id));
    this.showEditModal = true;
  }

  openMembersModal(group: MailGroup): void {
    this.selectedGroup = group;
    this.showMembersModal = true;
  }

  createGroup(): void {
    if (!this.groupForm.name) return;

    const request: GroupRequest = {
      name: this.groupForm.name,
      description: this.groupForm.description,
      memberIds: Array.from(this.selectedMemberIds)
    };

    this.loading = true;
    this.mailService.createGroup(request).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Group created successfully';
          this.loadGroups();
          this.closeModals();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error creating group:', error);
        this.errorMessage = error.error?.message || 'Failed to create group';
        this.loading = false;
      }
    });
  }

  updateGroup(): void {
    if (!this.editingGroup || !this.groupForm.name) return;

    const request: GroupRequest = {
      name: this.groupForm.name,
      description: this.groupForm.description,
      memberIds: Array.from(this.selectedMemberIds)
    };

    this.loading = true;
    this.mailService.updateGroup(this.editingGroup.id, request).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Group updated successfully';
          this.loadGroups();
          this.closeModals();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating group:', error);
        this.errorMessage = error.error?.message || 'Failed to update group';
        this.loading = false;
      }
    });
  }

  deleteGroup(group: MailGroup): void {
    if (!confirm(`Are you sure you want to delete "${group.name}"?`)) return;

    this.loading = true;
    this.mailService.deleteGroup(group.id).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Group deleted successfully';
          this.loadGroups();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error deleting group:', error);
        this.errorMessage = error.error?.message || 'Failed to delete group';
        this.loading = false;
      }
    });
  }

  removeMember(groupId: number, memberId: number): void {
    this.mailService.removeGroupMember(groupId, memberId).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Member removed successfully';
          this.loadGroups();
          if (this.selectedGroup) {
            this.selectedGroup = response.data;
          }
        }
      },
      error: (error) => {
        console.error('Error removing member:', error);
        this.errorMessage = error.error?.message || 'Failed to remove member';
      }
    });
  }

  isUserSelected(userId: number): boolean {
    return this.selectedMemberIds.has(userId);
  }

  toggleUser(userId: number): void {
    if (this.selectedMemberIds.has(userId)) {
      this.selectedMemberIds.delete(userId);
    } else {
      this.selectedMemberIds.add(userId);
    }
  }

  closeModals(): void {
    this.showCreateModal = false;
    this.showEditModal = false;
    this.showMembersModal = false;
    this.selectedGroup = null;
    this.editingGroup = null;
    this.errorMessage = '';
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }


  isGroupOwner(group: MailGroup): boolean {
    const currentUser = this.authService.getCurrentUser();
    return !!(currentUser && group.ownerId === currentUser.id);
  }

  getTotalMembersCount(): number {
  let total = 0;
  this.systemGroups.forEach(group => total += group.members.length);
  this.customGroups.forEach(group => total += group.members.length);
  return total;
}


}