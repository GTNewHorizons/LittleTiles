package com.creativemd.littletiles.common.gui;

import com.cleanroommc.modularui.widget.ParentWidget;

/**
 * The settings of one structure type in the recipe gui, see
 * {@link com.creativemd.littletiles.common.structure.LittleStructure#createConfigWidget}.
 */
public class StructureConfigWidget extends ParentWidget<StructureConfigWidget> {

    private Runnable onChange = () -> {};

    /** Runs whenever the settings change, so the recipe can save them right away. */
    public StructureConfigWidget onChange(Runnable onChange) {
        this.onChange = onChange;
        return this;
    }

    /** To be called by the settings whenever the structure they describe changes. */
    protected void changed() {
        onChange.run();
    }
}
