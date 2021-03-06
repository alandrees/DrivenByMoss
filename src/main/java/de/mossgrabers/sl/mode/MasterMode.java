// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.sl.mode;

import de.mossgrabers.framework.ButtonEvent;
import de.mossgrabers.framework.Model;
import de.mossgrabers.framework.controller.display.Display;
import de.mossgrabers.framework.daw.MasterTrackProxy;
import de.mossgrabers.framework.mode.AbstractMode;
import de.mossgrabers.sl.SLConfiguration;
import de.mossgrabers.sl.controller.SLControlSurface;


/**
 * Master mode.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class MasterMode extends AbstractMode<SLControlSurface, SLConfiguration>
{
    private static final String PARAM_NAMES = "Volume   Pan                                                     Master ";


    /**
     * Constructor.
     *
     * @param surface The surface
     * @param model The model
     */
    public MasterMode (final SLControlSurface surface, final Model model)
    {
        super (surface, model);
        this.isTemporary = false;
    }


    /** {@inheritDoc} */
    @Override
    public void updateDisplay ()
    {
        final Display d = this.surface.getDisplay ();
        final MasterTrackProxy master = this.model.getMasterTrack ();

        d.setRow (0, MasterMode.PARAM_NAMES).clearRow (2).setCell (2, 0, master.getVolumeStr ()).setCell (2, 1, master.getPanStr ()).done (2);
    }


    /** {@inheritDoc} */
    @Override
    public void onRowButton (final int row, final int index, final ButtonEvent event)
    {
        // Intentionally empty
    }
}