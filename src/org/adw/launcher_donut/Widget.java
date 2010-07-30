/*
*    Copyright 2010 AnderWeb (Gustavo Claramunt) <anderweb@gmail.com>
*
*    This file is part of ADW.Launcher.
*
*    ADW.Launcher is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    ADW.Launcher is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with ADW.Launcher.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.adw.launcher_donut;




/**
 * Represents one instance of a Launcher widget, such as search.
 */
class Widget extends ItemInfo {
    int layoutResource;

    static Widget makeSearch() {
        Widget w = new Widget();
        w.itemType = LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH;
        w.spanX = 4;
        w.spanY = 1;
        w.layoutResource = R.layout.widget_search;
        return w;
    }
}
