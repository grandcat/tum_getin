package com.tca.mobiledooraccess;

/**
 * Created by Stefan on 12.07.2015.
 */
public class ProgressStatusModel {
    public String label;
    public String description;
    /**
     * Resource ID for icon to show.
     */
    public int iconId;

    public ProgressStatusModel(int iconId, String label, String description) {
        this.iconId = iconId;
        this.label = label;
        this.description = description;
    }

    public ProgressStatusModel(String label, String description) {
        this(R.drawable.btn_check_buttonless_off, label, description);
    }

    public void setCheckIcon() {
        iconId = R.drawable.btn_check_buttonless_on;
    }

    public void setErrIcon() {
        iconId = R.drawable.ic_dialog_alert_holo_light;
    }

}
