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

package org.projectforge.admin;

import static org.projectforge.admin.SystemUpdater.CORE_REGION_ID;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.database.DatabaseUpdateDO;
import org.projectforge.database.DatabaseUpdateDao;
import org.projectforge.database.Table;
import org.projectforge.database.TableAttribute;
import org.projectforge.database.TableAttributeType;
import org.projectforge.registry.Registry;
import org.projectforge.user.UserDao;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DatabaseCoreUpdates
{
  @SuppressWarnings("serial")
  public static List<UpdateEntry> getUpdateEntries()
  {
    final List<UpdateEntry> list = new ArrayList<UpdateEntry>();
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "3.5.4",
        "Adds table t_database_update. Adds attribute (excel_)date_format, hour_format_24 to table t_pf_user.") {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        final DatabaseUpdateDao dao = SystemUpdater.instance().databaseUpdateDao;
        return this.preCheckStatus = dao.doesTableExist("t_database_update") == true
            && dao.doesTableAttributeExist("t_pf_user", "date_format") == true
            && dao.doesTableAttributeExist("t_pf_user", "excel_date_format") == true
            && dao.doesTableAttributeExist("t_pf_user", "time_notation") == true ? UpdatePreCheckStatus.ALREADY_UPDATED
            : UpdatePreCheckStatus.OK;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        final DatabaseUpdateDao dao = SystemUpdater.instance().databaseUpdateDao;
        if (dao.doesTableExist(DatabaseUpdateDO.TABLE_NAME) == true
            && dao.doesTableAttributeExist("t_pf_user", "date_format") == true
            && dao.doesTableAttributeExist("t_pf_user", "excel_date_format") == true
            && dao.doesTableAttributeExist("t_pf_user", "time_notation") == true) {
          return this.runningStatus = UpdateRunningStatus.DONE;
        }
        if (dao.doesTableExist(DatabaseUpdateDO.TABLE_NAME) == false) {
          final Table table = new Table(DatabaseUpdateDO.TABLE_NAME) //
              .addAttribute(new TableAttribute("update_date", TableAttributeType.TIMESTAMP)) //
              .addAttribute(new TableAttribute("region_id", TableAttributeType.VARCHAR, 1000)) //
              .addAttribute(new TableAttribute("version", TableAttributeType.VARCHAR, 15)) //
              .addAttribute(new TableAttribute("execution_result", TableAttributeType.VARCHAR, 1000)) //
              .addAttribute(
                  new TableAttribute("executed_by_user_fk", TableAttributeType.INT).setForeignTable("t_pf_user").setForeignAttribute("pk")) //
              .addAttribute(new TableAttribute("description", TableAttributeType.VARCHAR, 4000));
          dao.createTable(table);
        }
        if (dao.doesTableAttributeExist("t_pf_user", "date_format") == false) {
          dao.addTableAttributes("t_pf_user", new TableAttribute("date_format", TableAttributeType.VARCHAR, 20));
        }
        if (dao.doesTableAttributeExist("t_pf_user", "excel_date_format") == false) {
          dao.addTableAttributes("t_pf_user", new TableAttribute("excel_date_format", TableAttributeType.VARCHAR, 20));
        }
        if (dao.doesTableAttributeExist("t_pf_user", "time_notation") == false) {
          dao.addTableAttributes("t_pf_user", new TableAttribute("time_notation", TableAttributeType.VARCHAR, 6));
        }
        final UserDao userDao = (UserDao) Registry.instance().getDao(UserDao.class);
        userDao.getUserGroupCache().setExpired();
        return this.runningStatus = UpdateRunningStatus.DONE;
      }
    });
    return list;
  }
}
