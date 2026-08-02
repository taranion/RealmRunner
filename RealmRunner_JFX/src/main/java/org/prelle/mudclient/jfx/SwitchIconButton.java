package org.prelle.mudclient.jfx;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * 
 */
public class SwitchIconButton extends Button {
	
	private BooleanProperty prop;
	private ImageView ivOn, ivOff;

	//-------------------------------------------------------------------
	/**
	 */
	public SwitchIconButton(BooleanProperty prop, String iconOn, String iconOff, String tooltip) {
		this.prop = prop;
		Image  imgOn = new Image(getClass().getResourceAsStream(iconOn));
		Image imgOff = new Image(getClass().getResourceAsStream(iconOff));
		ivOn  = new ImageView(imgOn);
		ivOff = new ImageView(imgOff);
		setGraphic(prop.get() ? ivOn : ivOff);
		//setStyle("-fx-background-color: transparent; -fx-border: 0px;");
		this.setTooltip(new Tooltip(tooltip));
		prop.addListener( (_, _, newVal) -> {
			setGraphic(newVal ? ivOn : ivOff);
		});
		
		this.setAccessibleText(tooltip);
	}

}
