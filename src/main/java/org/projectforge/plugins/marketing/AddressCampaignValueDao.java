/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.plugins.marketing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.projectforge.core.BaseDao;
import org.projectforge.core.BaseSearchFilter;
import org.projectforge.core.QueryFilter;
import org.projectforge.user.UserRightId;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AddressCampaignValueDao extends BaseDao<AddressCampaignValueDO>
{
  public static final UserRightId USER_RIGHT_ID = new UserRightId("PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE", "unused", "unused");;

  public AddressCampaignValueDao()
  {
    super(AddressCampaignValueDO.class);
    userRightId = USER_RIGHT_ID;
  }

  @Override
  public List<AddressCampaignValueDO> getList(final BaseSearchFilter filter)
  {
    final AddressCampaignFilter myFilter;
    if (filter instanceof AddressCampaignFilter) {
      myFilter = (AddressCampaignFilter) filter;
    } else {
      myFilter = new AddressCampaignFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.getAddressCampaign() != null) {
      queryFilter.add(Restrictions.eq("address_campaign_fk", myFilter.getAddressCampaign().getId()));
    }
    return getList(queryFilter);
  }

  @Override
  public AddressCampaignValueDO newInstance()
  {
    return new AddressCampaignValueDO();
  }

  public Map<Integer, AddressCampaignValueDO> getAddressCampaignValuesByAddressId(final AddressCampaignFilter searchFilter)
  {
    return new HashMap<Integer, AddressCampaignValueDO>();
  }
}
