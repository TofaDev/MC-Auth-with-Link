package me.mastercapexd.auth.link.message.vk;

import java.util.List;
import java.util.stream.Collectors;

import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;

import me.mastercapexd.auth.link.message.keyboard.AbstractKeyboard;
import me.mastercapexd.auth.link.message.keyboard.button.Button;

public class VKKeyboard extends AbstractKeyboard {
	private boolean removeAfterClick;

	private VKKeyboard() {
	}

	public static VKKeyboardBuilder newBuilder() {
		return new VKKeyboard().new VKKeyboardBuilder();
	}

	@Override
	public VKButton[][] getButtons() {
		return buttons.stream().map(list -> list.toArray(new VKButton[0])).toArray(VKButton[][]::new);
	}

	public Keyboard buildKeyboard() {
		System.out.println(buttons);

		List<List<KeyboardButton>> buildedButtons = buttons.stream().map(buttonList -> {
			List<KeyboardButton> buttons = buttonList.stream().map(button -> button.as(VKButton.class)).map(VKButton::getKeyboardButton)
					.collect(Collectors.toList());
			System.out.println(buttons.size());
			buttons.stream().forEach(button -> System.out.println(button.toPrettyString()));
			return buttons;
		}).collect(Collectors.toList());

		System.out.println(buildedButtons);

		Keyboard keyboard = new Keyboard();
		keyboard.setInline(inline);
		keyboard.setOneTime(removeAfterClick);
		keyboard.setButtons(buildedButtons);
		return keyboard;
	}

	public class VKKeyboardBuilder implements IKeyboardBuilder {

		private VKKeyboardBuilder() {
		}

		@Override
		public IKeyboardBuilder button(int row, Button button) {
			VKKeyboard.this.addButton(row, button);
			return this;
		}

		@Override
		public IKeyboardBuilder inline(boolean inline) {
			VKKeyboard.this.inline = inline;
			return this;
		}

		@Override
		public VKKeyboard build() {
			return VKKeyboard.this;
		}

	}
}
