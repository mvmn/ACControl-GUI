package x.mvmn.aircndctrl.gui;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import x.mvmn.aircndctrl.gui.util.LangUtils;
import x.mvmn.aircndctrl.gui.util.LangUtils.SafeCaller;
import x.mvmn.aircndctrl.model.addr.ACAddress;
import x.mvmn.aircndctrl.model.addr.ACBinding;
import x.mvmn.aircndctrl.model.response.DiscoverResponse;
import x.mvmn.aircndctrl.model.response.SetParametersResponse;
import x.mvmn.aircndctrl.service.ACControlService;
import x.mvmn.aircndctrl.util.LangUtil;

public class ACController {
	protected final ACControlService controlService;
	protected final ACAddress acAddress;
	protected final DiscoverResponse discoverResponse;
	protected final Consumer<Throwable> exceptionHandler;
	protected final SafeCaller safeCaller;

	public ACController(ACControlService controlService, DiscoverResponse discoverResponse, ACAddress acAddress, Consumer<Throwable> exceptionHandler) {
		this.controlService = controlService;
		this.acAddress = acAddress;
		this.discoverResponse = discoverResponse;
		this.exceptionHandler = exceptionHandler;
		this.safeCaller = LangUtils.safeCaller(exceptionHandler);
	}

	public Map<String, Object> getStatus() throws IOException {
		return controlService.getStatus(bind()).getData().valuesMap();
	}

	public SetParametersResponse setValue(Map<String, Object> properties) throws IOException {
		return controlService.setParameters(bind(), properties).getData();
	}

	protected ACBinding bind() throws IOException {
		return ACBinding.ofBindResponse(controlService.bind(acAddress));
	}

	public DiscoverResponse getDiscoverResponse() {
		return discoverResponse;
	}

	protected SetParametersResponse setValue(String key, Object value) {
		return safeCaller.callSafe(() -> this.setValue(LangUtil.mapBuilder(key, value).build()));
	}

	public void onOff(boolean on) {
		setValue("Pow", (Object) (on ? 1 : 0));
	}
}
