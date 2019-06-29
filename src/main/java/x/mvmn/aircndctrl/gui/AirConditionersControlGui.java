package x.mvmn.aircndctrl.gui;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import x.mvmn.aircndctrl.gui.util.LangUtils;
import x.mvmn.aircndctrl.gui.util.LangUtils.SafeCaller;
import x.mvmn.aircndctrl.gui.util.SwingUtil;
import x.mvmn.aircndctrl.model.addr.ACAddress;
import x.mvmn.aircndctrl.service.ACControlService;
import x.mvmn.aircndctrl.service.ACDiscoverService;
import x.mvmn.aircndctrl.service.EncryptionService;
import x.mvmn.aircndctrl.service.impl.ACControlServiceImpl;
import x.mvmn.aircndctrl.service.impl.ACDiscoverServiceImpl;
import x.mvmn.aircndctrl.service.impl.EncryptionServiceImpl;

public class AirConditionersControlGui {

	public static void main(String[] args) {
		File dataFolder = new File(new File(System.getProperty("user.home")), ".acctrl");
		if (!dataFolder.exists()) {
			dataFolder.mkdir();
		}
		if (!SystemTray.isSupported()) {
			System.err.println("Warning: SystemTray is not supported. Program might not work.");
		}
		new AirConditionersControlGui().doDiscovery();
	}

	private static final Map<String, Object> CELSIUS_TEMPS;
	static {
		LinkedHashMap<String, Object> celsiusTemps = new LinkedHashMap<>();
		for (int i = 16; i <= 30; i++) {
			celsiusTemps.put(String.valueOf(i), i);
		}
		CELSIUS_TEMPS = Collections.unmodifiableMap(celsiusTemps);
	}
	private static final Map<String, Object> FAN_SPEEDS;
	static {
		LinkedHashMap<String, Object> fanModes = new LinkedHashMap<>();
		fanModes.put("Auto", 0);
		fanModes.put("Slowest", 1);
		fanModes.put("Slow", 2);
		fanModes.put("Medium", 3);
		fanModes.put("Higher", 4);
		fanModes.put("Highest", 5);
		FAN_SPEEDS = Collections.unmodifiableMap(fanModes);
	}

	private static final Map<String, Object> AC_MODES;
	static {
		LinkedHashMap<String, Object> acModes = new LinkedHashMap<>();
		acModes.put("Auto", 0);
		acModes.put("Cool", 1);
		acModes.put("Dry", 2);
		acModes.put("Fan", 3);
		acModes.put("Heat", 4);
		AC_MODES = Collections.unmodifiableMap(acModes);
	}

	private static final Map<String, Object> DIRECTIONS_HORIZONTAL;
	static {
		LinkedHashMap<String, Object> directions = new LinkedHashMap<>();
		directions.put("Current", 0);
		directions.put("Swing", 1);
		directions.put("Leftmost", 2);
		directions.put("Left", 3);
		directions.put("Center", 4);
		directions.put("Right", 5);
		directions.put("Rightmost", 6);
		DIRECTIONS_HORIZONTAL = Collections.unmodifiableMap(directions);
	}

	private static final Map<String, Object> DIRECTIONS_VERTICAL;
	static {
		LinkedHashMap<String, Object> directions = new LinkedHashMap<>();
		directions.put("Current", 0);
		directions.put("Swing: full", 1);
		directions.put("Upmost", 2);
		directions.put("Up", 3);
		directions.put("Middle", 4);
		directions.put("Down", 5);
		directions.put("Downmost", 6);
		directions.put("Swing: upmost", 11);
		directions.put("Swing: up", 10);
		directions.put("Swing: middle", 9);
		directions.put("Swing: down", 8);
		directions.put("Swing: downmost", 7);
		DIRECTIONS_VERTICAL = Collections.unmodifiableMap(directions);
	}

	protected final PopupMenu popup;
	private final EncryptionService encryptionService;
	private final ACControlService acControlService;
	private final ACDiscoverService acDiscoveryService;

	public AirConditionersControlGui() {
		encryptionService = new EncryptionServiceImpl();
		acControlService = new ACControlServiceImpl(encryptionService);
		acDiscoveryService = new ACDiscoverServiceImpl(encryptionService);

		TrayIcon trayIcon;
		try {
			trayIcon = new TrayIcon(ImageIO.read(AirConditionersControlGui.class.getResourceAsStream("logo.png")));
			trayIcon.setImageAutoSize(true);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		popup = new PopupMenu();

		// Add components to pop-up menu
		popup.add(SwingUtil.menuItem("About", event -> JOptionPane.showMessageDialog(null, "AirConditioner controller by Mykola Makhin")));
		popup.add(SwingUtil.menuItem("Rescan", event -> new Thread(() -> AirConditionersControlGui.this.doDiscovery()).start()));
		popup.addSeparator();
		popup.addSeparator();
		popup.add(SwingUtil.menuItem("Quit", event -> SystemTray.getSystemTray().remove(trayIcon)));

		trayIcon.setPopupMenu(popup);

		try {
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			// Show error and quit app
			ExceptionDisplayer.INSTANCE.accept(e);
			throw new RuntimeException(e);
		}
	}

	protected void doDiscovery() {
		while (popup.getItemCount() > 5) {
			popup.remove(3);
		}
		// CopyOnWriteArrayList<ACController> allDiscovered = new CopyOnWriteArrayList<>();
		// popup.insert(constructAcMenu(allDiscovered, "All"), 3);
		// popup.insertSeparator(4);
		try {
			Set<String> discoveredMacs = Collections.synchronizedSet(new HashSet<>());
			LangUtils.call(() -> acDiscoveryService.discover(15000, discovery -> {
				ACController controller = new ACController(acControlService, discovery.getData(), ACAddress.ofDiscoveryResponse(discovery),
						ExceptionDisplayer.INSTANCE);
				// allDiscovered.add(controller);
				synchronized (discoveredMacs) {
					if (!discoveredMacs.contains(discovery.getData().getMac())) {
						SwingUtilities.invokeLater(() -> addAcMenu(constructAcMenu(controller)));
						discoveredMacs.add(discovery.getData().getMac());
					}
				}
			}), ExceptionDisplayer.INSTANCE::accept);
		} catch (Throwable t) {
			ExceptionDisplayer.INSTANCE.accept(t);
		}
	}

	protected void addAcMenu(Menu acMenu) {
		popup.insert(acMenu, 3);
	}

	protected Menu constructAcMenu(ACController controller) {
		SafeCaller safeCaller = LangUtils.safeCaller(ExceptionDisplayer.INSTANCE);
		String name = controller.getDiscoverResponse().getName();
		Menu result = new Menu(name);
		Map<String, Object> status = Collections.synchronizedMap(safeCaller.callSafe(() -> controller.getStatus()));
		System.out.println(name + ": " + status);
		Callable<?> syncStatus = () -> controller.setValue(status);
		Runnable syncStatusSafe = () -> safeCaller.callSafe(syncStatus);
		Runnable syncStatusOffThread = () -> new Thread(syncStatusSafe).start();
		result.add(boolParamAcMenuOption("Power", "Pow", status, syncStatusOffThread));
		result.add(boolParamAcMenuOption("Indicator light", "Lig", status, syncStatusOffThread));
		result.add(optionsSubmenu("Temperature C", CELSIUS_TEMPS, "SetTem", status, syncStatusOffThread));
		result.add(optionsSubmenu("AC mode", AC_MODES, "Mod", status, syncStatusOffThread));
		result.add(boolParamAcMenuOption("Sleep mode", "SwhSlp", status, syncStatusOffThread));
		result.addSeparator();
		result.add(optionsSubmenu("Fan speed", FAN_SPEEDS, "WdSpd", status, syncStatusOffThread));
		result.add(boolParamAcMenuOption("Fan turbo", "Tur", status, syncStatusOffThread));
		result.add(boolParamAcMenuOption("X-Fan", "Blo", status, syncStatusOffThread));
		result.add(boolParamAcMenuOption("Quiet mode", "Quiet", status, syncStatusOffThread));
		result.addSeparator();
		result.add(optionsSubmenu("Air flow directions: vertical", DIRECTIONS_VERTICAL, "SwUpDn", status, syncStatusOffThread));
		result.add(optionsSubmenu("Air flow directions: horizontal ", DIRECTIONS_HORIZONTAL, "SwingLfRig", status, syncStatusOffThread));
		result.addSeparator();
		result.add(boolParamAcMenuOption("Fresh air", "Air", status, syncStatusOffThread));
		result.add(boolParamAcMenuOption("Ionization (Cold Plasma)", "Health", status, syncStatusOffThread));
		result.addSeparator();
		result.add(boolParamAcMenuOption("Freeze prevention heating", "StHt", status, syncStatusOffThread));
		result.add(boolParamAcMenuOption("Energy saving", "SvSt", status, syncStatusOffThread));

		return result;
	}

	protected static CheckboxMenuItem boolParamAcMenuOption(String name, String param, Map<String, Object> status, Runnable syncStatusOffThread) {
		return SwingUtil.checkboxMenuItem(name, LangUtils.INT_ONE.equals(status.get(param)), event -> {
			status.put(param, ((CheckboxMenuItem) event.getSource()).getState() ? 1 : 0);
			syncStatusOffThread.run();
		});
	}

	protected static Menu optionsSubmenu(String name, Map<String, Object> options, String param, Map<String, Object> status, Runnable syncStatusOffThread) {
		Menu menu = new Menu(name);

		Object currentVal = status.get(param);
		for (Map.Entry<String, Object> option : options.entrySet()) {
			CheckboxMenuItem menuItem = new CheckboxMenuItem(option.getKey(), option.getValue().equals(currentVal));
			menuItem.addItemListener(event -> {
				for (int i = 0; i < menu.getItemCount(); i++) {
					CheckboxMenuItem cbmi = (CheckboxMenuItem) menu.getItem(i);
					if (cbmi != menuItem) {
						cbmi.setState(false);
					}
				}
				status.put(param, option.getValue());
				syncStatusOffThread.run();
			});
			menu.add(menuItem);
		}

		return menu;
	}
}
