/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.projectforge.common.AbstractCache;
import org.projectforge.common.StringHelper;
import org.projectforge.fibu.EmployeeDO;
import org.projectforge.fibu.ProjektDO;
import org.projectforge.web.UserFilter;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * The group user relations will be cached with this class.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class UserGroupCache extends AbstractCache
{
  private static Logger log = Logger.getLogger(UserGroupCache.class);

  /** The key is the user id and the value is a list of assigned groups. */
  private Map<Integer, Set<Integer>> userGroupIdMap;

  private Map<Integer, GroupDO> groupMap;

  /**
   * List of all rights (value) defined for the user ids (key).
   */
  private Map<Integer, List<UserRightDO>> rightMap;

  private Map<Integer, PFUserDO> userMap;

  private Map<Integer, EmployeeDO> employeeMap;

  private Set<Integer> adminUsers;

  private Set<Integer> financeUsers;

  private Set<Integer> controllingUsers;

  private Set<Integer> projectManagers;

  private Set<Integer> projectAssistants;

  private Set<Integer> marketingUsers;

  private Set<Integer> orgaUsers;

  private HibernateTemplate hibernateTemplate;

  public void setHibernateTemplate(final HibernateTemplate hibernateTemplate)
  {
    this.hibernateTemplate = hibernateTemplate;
  }

  public GroupDO getGroup(final Integer groupId)
  {
    checkRefresh();
    return getGroupMap().get(groupId);
  }

  public GroupDO getGroup(final ProjectForgeGroup group)
  {
    checkRefresh();
    for (final GroupDO g : groupMap.values()) {
      if (group.equals(g.getName()) == true) {
        return g;
      }
    }
    return null;
  }

  public String getGroupname(final Integer groupId)
  {
    checkRefresh();
    final GroupDO group = getGroup(groupId);
    return group == null ? null : group.getName();
  }

  public PFUserDO getUser(final Integer userId)
  {
    if (userId == null) {
      return null;
    }
    // checkRefresh(); Done by getUserMap().
    return getUserMap() != null ? userMap.get(userId) : null; // Only null in maintenance mode (if t_user isn't readable).
  }

  public PFUserDO getUser(final String username)
  {
    if (StringUtils.isEmpty(username) == true) {
      return null;
    }
    for (final PFUserDO user : getUserMap().values()) {
      if (username.equals(user.getUsername()) == true) {
        return user;
      }
    }
    return null;
  }

  public PFUserDO getUserByFullname(final String fullname)
  {
    if (StringUtils.isEmpty(fullname) == true) {
      return null;
    }
    for (final PFUserDO user : getUserMap().values()) {
      if (fullname.equals(user.getFullname()) == true) {
        return user;
      }
    }
    return null;
  }

  /**
   * @return all users (also deleted users).
   */
  public Collection<PFUserDO> getAllUsers()
  {
    // checkRefresh(); Done by getUserMap().
    return getUserMap().values();
  }

  /**
   * @return all groups (also deleted groups).
   */
  public Collection<GroupDO> getAllGroups()
  {
    // checkRefresh(); Done by getGMap().
    return getGroupMap().values();
  }

  /**
   * Only for internal use.
   */
  public int internalGetNumberOfUsers()
  {
    if (userMap == null) {
      return 0;
    } else {
      // checkRefresh(); Done by getUserMap().
      return getUserMap().size();
    }
  }

  public String getUsername(final Integer userId)
  {
    // checkRefresh(); Done by getUserMap().
    final PFUserDO user = getUserMap().get(userId);
    if (user == null) {
      return String.valueOf(userId);
    }
    return user.getUsername();
  }

  /**
   * Check for current logged in user.
   * @param groupId
   * @return
   */
  public boolean isLoggedInUserMemberOfGroup(final Integer groupId)
  {
    return isUserMemberOfGroup(PFUserContext.getUserId(), groupId);
  }

  /**
   * @param groupId
   * @return
   */
  public boolean isUserMemberOfGroup(final PFUserDO user, final Integer groupId)
  {
    if (user == null) {
      return false;
    }
    return isUserMemberOfGroup(user.getId(), groupId);
  }

  public boolean isUserMemberOfGroup(final Integer userId, final Integer groupId)
  {
    if (groupId == null) {
      return false;
    }
    checkRefresh();
    final Set<Integer> groupSet = getUserGroupIdMap().get(userId);
    return (groupSet != null) ? groupSet.contains(groupId) : false;
  }

  public boolean isUserMemberOfAtLeastOneGroup(final Integer userId, final Integer... groupIds)
  {
    if (groupIds == null) {
      return false;
    }
    checkRefresh();
    final Set<Integer> groupSet = getUserGroupIdMap().get(userId);
    if (groupSet == null) {
      return false;
    }
    for (final Integer groupId : groupIds) {
      if (groupId == null) {
        continue;
      }
      if (groupSet.contains(groupId) == true) {
        return true;
      }
    }
    return false;
  }

  public boolean isUserMemberOfAdminGroup()
  {
    return isUserMemberOfAdminGroup(PFUserContext.getUserId());
  }

  public boolean isUserMemberOfAdminGroup(final Integer userId)
  {
    checkRefresh();
    // adminUsers should only be null in maintenance mode (e. g. if user table isn't readable).
    return adminUsers != null ? adminUsers.contains(userId) : false;
  }

  public boolean isUserMemberOfFinanceGroup()
  {
    return isUserMemberOfFinanceGroup(PFUserContext.getUserId());
  }

  public boolean isUserMemberOfFinanceGroup(final Integer userId)
  {
    checkRefresh();
    // financeUsers should only be null in maintenance mode (e. g. if user table isn't readable).
    return financeUsers != null ? financeUsers.contains(userId) : false;
  }

  public boolean isUserMemberOfProjectManagers()
  {
    return isUserMemberOfProjectManagers(PFUserContext.getUserId());
  }

  public boolean isUserMemberOfProjectManagers(final Integer userId)
  {
    checkRefresh();
    // projectManagers should only be null in maintenance mode (e. g. if user table isn't readable).
    return projectManagers != null ? projectManagers.contains(userId) : false;
  }

  public boolean isUserMemberOfProjectAssistant()
  {
    return isUserMemberOfProjectAssistant(PFUserContext.getUserId());
  }

  public boolean isUserMemberOfProjectAssistant(final Integer userId)
  {
    checkRefresh();
    // projectAssistants should only be null in maintenance mode (e. g. if user table isn't readable).
    return projectAssistants != null ? projectAssistants.contains(userId) : false;
  }

  public boolean isUserProjectManagerOrAssistantForProject(final ProjektDO projekt)
  {
    if (projekt == null || projekt.getProjektManagerGroupId() == null) {
      return false;
    }
    final Integer userId = PFUserContext.getUserId();
    if (isUserMemberOfProjectAssistant(userId) == false && isUserMemberOfProjectManagers(userId) == false) {
      return false;
    }
    return isUserMemberOfGroup(userId, projekt.getProjektManagerGroupId());
  }

  public boolean isUserMemberOfControllingGroup()
  {
    return isUserMemberOfControllingGroup(PFUserContext.getUserId());
  }

  public boolean isUserMemberOfControllingGroup(final Integer userId)
  {
    checkRefresh();
    // controllingUsers should only be null in maintenance mode (e. g. if user table isn't readable).
    return controllingUsers != null ? controllingUsers.contains(userId) : false;
  }

  public boolean isUserMemberOfMarketingGroup()
  {
    return isUserMemberOfMarketingGroup(PFUserContext.getUserId());
  }

  public boolean isUserMemberOfMarketingGroup(final Integer userId)
  {
    checkRefresh();
    return marketingUsers.contains(userId);
  }

  public boolean isUserMemberOfOrgaGroup()
  {
    return isUserMemberOfOrgaGroup(PFUserContext.getUserId());
  }

  public boolean isUserMemberOfOrgaGroup(final Integer userId)
  {
    checkRefresh();
    // orgaUsers should only be null in maintenance mode (e. g. if user table isn't readable).
    return orgaUsers != null ? orgaUsers.contains(userId) : false;
  }

  /**
   * Checks if the given user is at least member of one of the given groups.
   * @param user
   * @param groups
   */
  public boolean isUserMemberOfGroup(final PFUserDO user, final ProjectForgeGroup... groups)
  {
    Validate.notNull(user);
    Validate.notNull(groups);
    for (final ProjectForgeGroup group : groups) {
      boolean result = false;
      if (group == ProjectForgeGroup.ADMIN_GROUP) {
        result = isUserMemberOfAdminGroup(user.getId());
      } else if (group == ProjectForgeGroup.FINANCE_GROUP) {
        result = isUserMemberOfFinanceGroup(user.getId());
      } else if (group == ProjectForgeGroup.PROJECT_MANAGER) {
        result = isUserMemberOfProjectManagers(user.getId());
      } else if (group == ProjectForgeGroup.PROJECT_ASSISTANT) {
        result = isUserMemberOfProjectAssistant(user.getId());
      } else if (group == ProjectForgeGroup.CONTROLLING_GROUP) {
        result = isUserMemberOfControllingGroup(user.getId());
      } else if (group == ProjectForgeGroup.MARKETING_GROUP) {
        result = isUserMemberOfMarketingGroup(user.getId());
      } else if (group == ProjectForgeGroup.ORGA_TEAM) {
        result = isUserMemberOfOrgaGroup(user.getId());
      } else {
        throw new UnsupportedOperationException("Group not yet supported: " + group);
      }
      if (result == true) {
        return true;
      }
    }
    return false;
  }

  public String getGroupnames(final Integer userId)
  {
    checkRefresh();
    final Set<Integer> groupSet = getUserGroupIdMap().get(userId);
    if (groupSet == null) {
      return "";
    }
    final List<String> list = new ArrayList<String>();
    for (final Integer groupId : groupSet) {
      final GroupDO group = getGroup(groupId);
      if (group != null) {
        list.add(group.getName());
      } else {
        log.error("Group with id " + groupId + " not found.");
      }
    }
    return StringHelper.listToString(list, "; ", true);
  }

  public List<UserRightDO> getUserRights(final Integer userId)
  {
    return getUserRightMap().get(userId);
  }

  private Map<Integer, List<UserRightDO>> getUserRightMap()
  {
    checkRefresh();
    return rightMap;
  }

  /**
   * Returns a collection of group id's to which the user is assigned to.
   * @param user
   * @return collection if found, otherwise null.
   */
  public Collection<Integer> getUserGroups(final PFUserDO user)
  {
    checkRefresh();
    return getUserGroupIdMap().get(user.getId());
  }

  public EmployeeDO getEmployee(final Integer userId)
  {
    checkRefresh();
    EmployeeDO employee = this.employeeMap.get(userId);
    if (employee == null) {
      @SuppressWarnings("unchecked")
      final List<EmployeeDO> list = this.hibernateTemplate.find("from EmployeeDO e where e.user.id = ?", userId);
      if (list != null && list.size() > 0) {
        employee = list.get(0);
        this.employeeMap.put(userId, employee);
      }
    }
    return employee;
  }

  /**
   * Removes given employee from map, so refresh for next access is forced.
   * @param userId
   */
  public void refreshEmployee(final Integer userId)
  {
    if (this.employeeMap != null) {
      this.employeeMap.remove(userId);
    }
  }

  private Map<Integer, GroupDO> getGroupMap()
  {
    checkRefresh();
    return groupMap;
  }

  private Map<Integer, Set<Integer>> getUserGroupIdMap()
  {
    checkRefresh();
    return userGroupIdMap;
  }

  /**
   * Should be called after user modifications.
   * @param user
   */
  void updateUser(final PFUserDO user)
  {
    getUserMap().put(user.getId(), user);
  }

  private Map<Integer, PFUserDO> getUserMap()
  {
    checkRefresh();
    return userMap;
  }

  /**
   * This method will be called by CacheHelper and is synchronized.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void refresh()
  {
    log.info("Initializing UserGroupCache ...");
    // This method must not be synchronized because it works with a new copy of maps.
    final Map<Integer, PFUserDO> uMap = new HashMap<Integer, PFUserDO>();
    // Could not autowire UserDao because of cyclic reference with AccessChecker.
    final List<PFUserDO> users = Login.getInstance().getAllUsers();
    for (final PFUserDO user : users) {
      uMap.put(user.getId(), user);
    }
    final List<GroupDO> groups = Login.getInstance().getAllGroups();
    final Map<Integer, GroupDO> gMap = new HashMap<Integer, GroupDO>();
    final Map<Integer, Set<Integer>> ugIdMap = new HashMap<Integer, Set<Integer>>();
    final Set<Integer> nAdminUsers = new HashSet<Integer>();
    final Set<Integer> nFinanceUser = new HashSet<Integer>();
    final Set<Integer> nControllingUsers = new HashSet<Integer>();
    final Set<Integer> nProjectManagers = new HashSet<Integer>();
    final Set<Integer> nProjectAssistants = new HashSet<Integer>();
    final Set<Integer> nMarketingUsers = new HashSet<Integer>();
    final Set<Integer> nOrgaUsers = new HashSet<Integer>();
    for (final GroupDO group : groups) {
      gMap.put(group.getId(), group);
      if (group.getAssignedUsers() != null) {
        for (final PFUserDO user : group.getAssignedUsers()) {
          if (user != null) {
            final Set<Integer> groupIdSet = ensureAndGetUserGroupIdMap(ugIdMap, user.getId());
            groupIdSet.add(group.getId());
            if (ProjectForgeGroup.ADMIN_GROUP.equals(group.getName()) == true) {
              log.debug("Adding user '" + user.getUsername() + "' as administrator.");
              nAdminUsers.add(user.getId());
            } else if (ProjectForgeGroup.FINANCE_GROUP.equals(group.getName()) == true) {
              log.debug("Adding user '" + user.getUsername() + "' for finance.");
              nFinanceUser.add(user.getId());
            } else if (ProjectForgeGroup.CONTROLLING_GROUP.equals(group.getName()) == true) {
              log.debug("Adding user '" + user.getUsername() + "' for controlling.");
              nControllingUsers.add(user.getId());
            } else if (ProjectForgeGroup.PROJECT_MANAGER.equals(group.getName()) == true) {
              log.debug("Adding user '" + user.getUsername() + "' as project manager.");
              nProjectManagers.add(user.getId());
            } else if (ProjectForgeGroup.PROJECT_ASSISTANT.equals(group.getName()) == true) {
              log.debug("Adding user '" + user.getUsername() + "' as project assistant.");
              nProjectAssistants.add(user.getId());
            } else if (ProjectForgeGroup.MARKETING_GROUP.equals(group.getName()) == true) {
              log.debug("Adding user '" + user.getUsername() + "' as marketing user.");
              nMarketingUsers.add(user.getId());
            } else if (ProjectForgeGroup.ORGA_TEAM.equals(group.getName()) == true) {
              log.debug("Adding user '" + user.getUsername() + "' as orga user.");
              nOrgaUsers.add(user.getId());
            }
          }
        }
      }
    }
    this.userMap = uMap;
    this.groupMap = gMap;
    this.adminUsers = nAdminUsers;
    this.financeUsers = nFinanceUser;
    this.controllingUsers = nControllingUsers;
    this.projectManagers = nProjectManagers;
    this.projectAssistants = nProjectAssistants;
    this.marketingUsers = nMarketingUsers;
    this.orgaUsers = nOrgaUsers;
    this.userGroupIdMap = ugIdMap;
    this.employeeMap = new HashMap<Integer, EmployeeDO>();
    final Map<Integer, List<UserRightDO>> rMap = new HashMap<Integer, List<UserRightDO>>();
    List<UserRightDO> rights;
    try {
      rights = hibernateTemplate.find("from UserRightDO t order by user.id, right_id");
    } catch (final Exception ex) {
      log.fatal("******* Exception while getting user rights from data-base (only OK for migration from older versions): "
          + ex.getMessage());
      rights = new ArrayList<UserRightDO>();
    }
    List<UserRightDO> list = null;
    Integer userId = null;
    for (final UserRightDO right : rights) {
      if (right.getUserId() == null) {
        log.warn("Oups, userId = null: " + right);
        continue;
      }
      if (right.getUserId().equals(userId) == false) {
        list = new ArrayList<UserRightDO>();
        userId = right.getUserId();
        if (userId != null) {
          rMap.put(userId, list);
        }
      }
      if (UserRights.instance().getRight(right.getRightId()).isAvailable(this, right.getUser()) == true) {
        list.add(right);
      }
    }
    this.rightMap = rMap;
    log.info("Initializing of UserGroupCache done.");
    Login.getInstance().afterUserGroupCacheRefresh(users, groups);
  }

  private static Set<Integer> ensureAndGetUserGroupIdMap(final Map<Integer, Set<Integer>> ugIdMap, final Integer userId)
  {
    Set<Integer> set = ugIdMap.get(userId);
    if (set == null) {
      set = new HashSet<Integer>();
      ugIdMap.put(userId, set);
    }
    return set;
  }

  public synchronized void internalSetAdminUser(final PFUserDO adminUser)
  {
    if (UserFilter.isUpdateRequiredFirst() == false) {
      throw new IllegalStateException(
          "Can't set admin user internally! This method is only available if system is under maintenance (update required first is true)!");
    }
    checkRefresh();
    this.adminUsers.add(adminUser.getId());
  }
}
