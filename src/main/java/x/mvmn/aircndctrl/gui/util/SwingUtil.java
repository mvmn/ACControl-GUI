package x.mvmn.aircndctrl.gui.util;

import java.awt.CheckboxMenuItem;
import java.awt.MenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

public class SwingUtil {
	public static MenuItem menuItem(String text, ActionListener handler) {
		MenuItem menuItem = new MenuItem(text);
		menuItem.addActionListener(handler);
		return menuItem;
	}

	public static CheckboxMenuItem checkboxMenuItem(String text, boolean state, ItemListener handler) {
		CheckboxMenuItem menuItem = new CheckboxMenuItem(text, state);
		menuItem.addItemListener(handler);
		return menuItem;
	}
}
