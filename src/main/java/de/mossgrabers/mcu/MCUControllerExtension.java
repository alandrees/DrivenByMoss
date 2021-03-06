// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.mcu;

import de.mossgrabers.framework.Model;
import de.mossgrabers.framework.command.Commands;
import de.mossgrabers.framework.command.continuous.KnobRowModeCommand;
import de.mossgrabers.framework.command.trigger.ButtonRowModeCommand;
import de.mossgrabers.framework.command.trigger.LoopCommand;
import de.mossgrabers.framework.command.trigger.MetronomeCommand;
import de.mossgrabers.framework.command.trigger.ModeMultiSelectCommand;
import de.mossgrabers.framework.command.trigger.ModeSelectCommand;
import de.mossgrabers.framework.command.trigger.MoveTrackBankCommand;
import de.mossgrabers.framework.command.trigger.NewCommand;
import de.mossgrabers.framework.command.trigger.NopCommand;
import de.mossgrabers.framework.command.trigger.PlayCommand;
import de.mossgrabers.framework.command.trigger.PunchInCommand;
import de.mossgrabers.framework.command.trigger.PunchOutCommand;
import de.mossgrabers.framework.command.trigger.RecordCommand;
import de.mossgrabers.framework.command.trigger.StopCommand;
import de.mossgrabers.framework.command.trigger.TapTempoCommand;
import de.mossgrabers.framework.command.trigger.ToggleTrackBanksCommand;
import de.mossgrabers.framework.command.trigger.UndoCommand;
import de.mossgrabers.framework.command.trigger.WindCommand;
import de.mossgrabers.framework.controller.AbstractControllerExtension;
import de.mossgrabers.framework.controller.color.ColorManager;
import de.mossgrabers.framework.daw.AbstractTrackBankProxy;
import de.mossgrabers.framework.daw.CursorDeviceProxy;
import de.mossgrabers.framework.daw.EffectTrackBankProxy;
import de.mossgrabers.framework.daw.MasterTrackProxy;
import de.mossgrabers.framework.daw.TrackBankProxy;
import de.mossgrabers.framework.daw.TransportProxy;
import de.mossgrabers.framework.daw.data.TrackData;
import de.mossgrabers.framework.midi.MidiInput;
import de.mossgrabers.framework.midi.MidiOutput;
import de.mossgrabers.framework.mode.ModeManager;
import de.mossgrabers.framework.view.View;
import de.mossgrabers.framework.view.ViewManager;
import de.mossgrabers.mcu.command.continuous.PlayPositionTempoCommand;
import de.mossgrabers.mcu.command.pitchbend.PitchbendVolumeCommand;
import de.mossgrabers.mcu.command.trigger.AssignableCommand;
import de.mossgrabers.mcu.command.trigger.AutomationCommand;
import de.mossgrabers.mcu.command.trigger.BrowserCommand;
import de.mossgrabers.mcu.command.trigger.CursorCommand;
import de.mossgrabers.mcu.command.trigger.CursorCommand.Direction;
import de.mossgrabers.mcu.command.trigger.GrooveCommand;
import de.mossgrabers.mcu.command.trigger.KeyCommand;
import de.mossgrabers.mcu.command.trigger.KeyCommand.Key;
import de.mossgrabers.mcu.command.trigger.OverdubCommand;
import de.mossgrabers.mcu.command.trigger.PaneCommand;
import de.mossgrabers.mcu.command.trigger.ScrubCommand;
import de.mossgrabers.mcu.command.trigger.SelectCommand;
import de.mossgrabers.mcu.command.trigger.SendSelectCommand;
import de.mossgrabers.mcu.command.trigger.ShiftCommand;
import de.mossgrabers.mcu.command.trigger.TempoTicksCommand;
import de.mossgrabers.mcu.command.trigger.ToggleDisplayCommand;
import de.mossgrabers.mcu.command.trigger.ZoomCommand;
import de.mossgrabers.mcu.controller.MCUControlSurface;
import de.mossgrabers.mcu.controller.MCUDisplay;
import de.mossgrabers.mcu.controller.MCUMidiInput;
import de.mossgrabers.mcu.controller.MCUSegmentDisplay;
import de.mossgrabers.mcu.controller.MCUValueChanger;
import de.mossgrabers.mcu.mode.Modes;
import de.mossgrabers.mcu.mode.device.DeviceBrowserMode;
import de.mossgrabers.mcu.mode.device.DeviceParamsMode;
import de.mossgrabers.mcu.mode.track.MasterMode;
import de.mossgrabers.mcu.mode.track.PanMode;
import de.mossgrabers.mcu.mode.track.SendMode;
import de.mossgrabers.mcu.mode.track.TrackMode;
import de.mossgrabers.mcu.mode.track.VolumeMode;
import de.mossgrabers.mcu.view.ControlView;
import de.mossgrabers.mcu.view.Views;

import com.bitwig.extension.controller.api.ControllerHost;

import java.util.Arrays;


/**
 * Bitwig Studio extension to support the Mackie MCU protocol.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class MCUControllerExtension extends AbstractControllerExtension<MCUControlSurface, MCUConfiguration>
{
    /** State for button LED on. */
    public static final int      MCU_BUTTON_STATE_ON       = 127;
    /** State for button LED off. */
    public static final int      MCU_BUTTON_STATE_OFF      = 0;

    private static final Integer COMMAND_NOTE_EDITOR       = Integer.valueOf (150);
    private static final Integer COMMAND_AUTOMATION_EDITOR = Integer.valueOf (151);
    private static final Integer COMMAND_TOGGLE_DEVICE     = Integer.valueOf (152);
    private static final Integer COMMAND_MIXER             = Integer.valueOf (153);
    private static final Integer COMMAND_TEMPO_TICKS       = Integer.valueOf (154);
    private static final Integer COMMAND_ENTER             = Integer.valueOf (155);
    private static final Integer COMMAND_CANCEL            = Integer.valueOf (156);
    private static final Integer COMMAND_FLIP              = Integer.valueOf (157);
    private static final Integer COMMAND_GROOVE            = Integer.valueOf (158);
    private static final Integer COMMAND_OVERDUB           = Integer.valueOf (159);
    private static final Integer COMMAND_SCRUB             = Integer.valueOf (160);
    private static final Integer COMMAND_FOOTSWITCH1       = Integer.valueOf (161);
    private static final Integer COMMAND_FOOTSWITCH2       = Integer.valueOf (162);
    private static final Integer COMMAND_F1                = Integer.valueOf (163);
    private static final Integer COMMAND_F2                = Integer.valueOf (164);
    private static final Integer COMMAND_F3                = Integer.valueOf (165);
    private static final Integer COMMAND_F4                = Integer.valueOf (166);
    private static final Integer COMMAND_F5                = Integer.valueOf (167);
    private static final Integer COMMAND_TOGGLE_DISPLAY    = Integer.valueOf (168);

    private final int []         vuValues                  = new int [10];
    private final int []         faderValues               = new int [9];


    /**
     * Constructor.
     *
     * @param extensionDefinition The extension definition
     * @param host The Bitwig host
     */
    public MCUControllerExtension (final MCUControllerExtensionDefinition extensionDefinition, final ControllerHost host)
    {
        super (extensionDefinition, host);

        Arrays.fill (this.vuValues, -1);
        Arrays.fill (this.faderValues, -1);

        this.colorManager = new ColorManager ();
        this.valueChanger = new MCUValueChanger (16241 + 1, 100, 10);
        this.configuration = new MCUConfiguration (this.valueChanger);
    }


    /** {@inheritDoc} */
    @Override
    public void flush ()
    {
        this.surface.flush ();

        this.updateButtons ();
        this.updateMode (this.surface.getModeManager ().getActiveModeId ());
    }


    /** {@inheritDoc} */
    @Override
    protected void createModel ()
    {
        this.model = new Model (this.getHost (), this.colorManager, this.valueChanger, this.scales, 8, 8, 8, 8, 8, true, -1, -1, -1, -1);

        final TrackBankProxy trackBank = this.model.getTrackBank ();
        trackBank.setIndication (true);
        trackBank.addTrackSelectionObserver (this::handleTrackChange);

        this.model.getMasterTrack ().addTrackSelectionObserver ( (index, isSelected) -> {
            if (isSelected)
                this.surface.getModeManager ().setActiveMode (Modes.MODE_MASTER);
            else
                this.surface.getModeManager ().restoreMode ();
        });
    }


    /** {@inheritDoc} */
    @Override
    protected void createSurface ()
    {
        final ControllerHost host = this.getHost ();
        final MidiOutput output = new MidiOutput (host);
        final MidiInput input = new MCUMidiInput ();
        this.surface = new MCUControlSurface (host, this.colorManager, this.configuration, output, input);
        this.surface.setDisplay (new MCUDisplay (host, output, true));
        this.surface.setSecondDisplay (new MCUDisplay (host, output, false));
        this.surface.setSegmentDisplay (new MCUSegmentDisplay (host, output));

        this.surface.getModeManager ().setDefaultMode (Modes.MODE_TRACK);
    }


    /** {@inheritDoc} */
    @Override
    protected void createModes ()
    {
        final ModeManager modeManager = this.surface.getModeManager ();

        modeManager.registerMode (Modes.MODE_TRACK, new TrackMode (this.surface, this.model));
        modeManager.registerMode (Modes.MODE_VOLUME, new VolumeMode (this.surface, this.model));
        modeManager.registerMode (Modes.MODE_PAN, new PanMode (this.surface, this.model));
        final SendMode modeSend = new SendMode (this.surface, this.model);
        for (int i = 0; i < 8; i++)
            modeManager.registerMode (Integer.valueOf (Modes.MODE_SEND1.intValue () + i), modeSend);

        modeManager.registerMode (Modes.MODE_DEVICE_PARAMS, new DeviceParamsMode (this.surface, this.model));
        modeManager.registerMode (Modes.MODE_BROWSER, new DeviceBrowserMode (this.surface, this.model));

        modeManager.registerMode (Modes.MODE_MASTER, new MasterMode (this.surface, this.model, false));
    }


    /** {@inheritDoc} */
    @Override
    protected void createObservers ()
    {
        this.surface.getModeManager ().addModeListener ( (oldMode, newMode) -> {
            this.updateMode (null);
            this.updateMode (newMode);
        });
    }


    /** {@inheritDoc} */
    @Override
    protected void createViews ()
    {
        final ViewManager viewManager = this.surface.getViewManager ();
        viewManager.registerView (Views.VIEW_CONTROL, new ControlView (this.surface, this.model));
    }


    /** {@inheritDoc} */
    @Override
    protected void registerTriggerCommands ()
    {
        // Footswitches
        this.addTriggerCommand (COMMAND_FOOTSWITCH1, MCUControlSurface.MCU_USER_A, new AssignableCommand (0, this.model, this.surface));
        this.addTriggerCommand (COMMAND_FOOTSWITCH2, MCUControlSurface.MCU_USER_B, new AssignableCommand (1, this.model, this.surface));

        // Navigation
        this.addTriggerCommand (Commands.COMMAND_REWIND, MCUControlSurface.MCU_REWIND, new WindCommand<> (this.model, this.surface, false));
        this.addTriggerCommand (Commands.COMMAND_FORWARD, MCUControlSurface.MCU_FORWARD, new WindCommand<> (this.model, this.surface, true));
        this.addTriggerCommand (Commands.COMMAND_LOOP, MCUControlSurface.MCU_REPEAT, new LoopCommand<> (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_STOP, MCUControlSurface.MCU_STOP, new StopCommand<> (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_PLAY, MCUControlSurface.MCU_PLAY, new PlayCommand<> (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_RECORD, MCUControlSurface.MCU_RECORD, new RecordCommand<> (this.model, this.surface));

        this.addTriggerCommand (COMMAND_SCRUB, MCUControlSurface.MCU_SCRUB, new ScrubCommand (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_ARROW_LEFT, MCUControlSurface.MCU_ARROW_LEFT, new CursorCommand (Direction.LEFT, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_ARROW_RIGHT, MCUControlSurface.MCU_ARROW_RIGHT, new CursorCommand (Direction.RIGHT, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_ARROW_UP, MCUControlSurface.MCU_ARROW_UP, new CursorCommand (Direction.UP, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_ARROW_DOWN, MCUControlSurface.MCU_ARROW_DOWN, new CursorCommand (Direction.DOWN, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_ZOOM, MCUControlSurface.MCU_ZOOM, new ZoomCommand (this.model, this.surface));

        // Display Mode
        this.addTriggerCommand (COMMAND_TOGGLE_DISPLAY, MCUControlSurface.MCU_NAME_VALUE, new ToggleDisplayCommand (this.model, this.surface));
        this.addTriggerCommand (COMMAND_TEMPO_TICKS, MCUControlSurface.MCU_SMPTE_BEATS, new TempoTicksCommand (this.model, this.surface));

        // Functions
        this.addTriggerCommand (Commands.COMMAND_SHIFT, MCUControlSurface.MCU_SHIFT, new ShiftCommand (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_SELECT, MCUControlSurface.MCU_OPTION, new NopCommand<> (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_PUNCH_IN, MCUControlSurface.MCU_F6, new PunchInCommand<> (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_PUNCH_OUT, MCUControlSurface.MCU_F7, new PunchOutCommand<> (this.model, this.surface));
        this.addTriggerCommand (COMMAND_F1, MCUControlSurface.MCU_F1, new AssignableCommand (2, this.model, this.surface));
        this.addTriggerCommand (COMMAND_F2, MCUControlSurface.MCU_F2, new AssignableCommand (3, this.model, this.surface));
        this.addTriggerCommand (COMMAND_F3, MCUControlSurface.MCU_F3, new AssignableCommand (4, this.model, this.surface));
        this.addTriggerCommand (COMMAND_F4, MCUControlSurface.MCU_F4, new AssignableCommand (5, this.model, this.surface));
        this.addTriggerCommand (COMMAND_F5, MCUControlSurface.MCU_F5, new AssignableCommand (6, this.model, this.surface));

        // Assignment
        this.addTriggerCommand (Commands.COMMAND_TRACK, MCUControlSurface.MCU_MODE_IO, new ModeMultiSelectCommand<> (this.model, this.surface, Modes.MODE_TRACK, Modes.MODE_VOLUME));
        this.addTriggerCommand (Commands.COMMAND_PAN_SEND, MCUControlSurface.MCU_MODE_PAN, new ModeSelectCommand<> (Modes.MODE_PAN, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_SENDS, MCUControlSurface.MCU_MODE_SENDS, new SendSelectCommand (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_DEVICE, MCUControlSurface.MCU_MODE_PLUGIN, new ModeSelectCommand<> (Modes.MODE_DEVICE_PARAMS, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_MOVE_TRACK_LEFT, MCUControlSurface.MCU_MODE_EQ, new MoveTrackBankCommand<> (this.model, this.surface, true, true));
        this.addTriggerCommand (Commands.COMMAND_MOVE_TRACK_RIGHT, MCUControlSurface.MCU_MODE_DYN, new MoveTrackBankCommand<> (this.model, this.surface, true, false));

        // Automation
        this.addTriggerCommand (Commands.COMMAND_AUTOMATION_READ, MCUControlSurface.MCU_READ, new AutomationCommand (0, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_AUTOMATION_WRITE, MCUControlSurface.MCU_WRITE, new AutomationCommand (1, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_AUTOMATION_TRIM, MCUControlSurface.MCU_TRIM, new AutomationCommand (2, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_AUTOMATION_TOUCH, MCUControlSurface.MCU_TOUCH, new AutomationCommand (3, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_AUTOMATION_LATCH, MCUControlSurface.MCU_LATCH, new AutomationCommand (4, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_UNDO, MCUControlSurface.MCU_UNDO, new UndoCommand<> (this.model, this.surface));

        // Utilities
        this.addTriggerCommand (COMMAND_NOTE_EDITOR, MCUControlSurface.MCU_MIDI_TRACKS, new PaneCommand (0, this.model, this.surface));
        this.addTriggerCommand (COMMAND_AUTOMATION_EDITOR, MCUControlSurface.MCU_INPUTS, new PaneCommand (1, this.model, this.surface));
        this.addTriggerCommand (COMMAND_TOGGLE_DEVICE, MCUControlSurface.MCU_AUDIO_TRACKS, new PaneCommand (2, this.model, this.surface));
        this.addTriggerCommand (COMMAND_MIXER, MCUControlSurface.MCU_AUDIO_INSTR, new PaneCommand (3, this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_BROWSE, MCUControlSurface.MCU_USER, new BrowserCommand (this.model, this.surface));
        this.addTriggerCommand (Commands.COMMAND_METRONOME, MCUControlSurface.MCU_CLICK, new MetronomeCommand<> (this.model, this.surface));
        this.addTriggerCommand (COMMAND_GROOVE, MCUControlSurface.MCU_SOLO, new GrooveCommand (this.model, this.surface));
        this.addTriggerCommand (COMMAND_OVERDUB, MCUControlSurface.MCU_REPLACE, new OverdubCommand (this.model, this.surface));

        // Fader Controls
        this.addTriggerCommand (COMMAND_FLIP, MCUControlSurface.MCU_FLIP, new ToggleTrackBanksCommand<> (this.model, this.surface));
        this.addTriggerCommand (COMMAND_CANCEL, MCUControlSurface.MCU_CANCEL, new KeyCommand (Key.ESCAPE, this.model, this.surface));
        this.addTriggerCommand (COMMAND_ENTER, MCUControlSurface.MCU_ENTER, new KeyCommand (Key.ENTER, this.model, this.surface));

        this.addTriggerCommand (Commands.COMMAND_MOVE_BANK_LEFT, MCUControlSurface.MCU_BANK_LEFT, new MoveTrackBankCommand<> (this.model, this.surface, false, true));
        this.addTriggerCommand (Commands.COMMAND_MOVE_BANK_RIGHT, MCUControlSurface.MCU_BANK_RIGHT, new MoveTrackBankCommand<> (this.model, this.surface, false, false));
        this.addTriggerCommand (Commands.COMMAND_MOVE_TRACK_LEFT, MCUControlSurface.MCU_TRACK_LEFT, new MoveTrackBankCommand<> (this.model, this.surface, true, true));
        this.addTriggerCommand (Commands.COMMAND_MOVE_TRACK_RIGHT, MCUControlSurface.MCU_TRACK_RIGHT, new MoveTrackBankCommand<> (this.model, this.surface, true, false));

        // Common track editing
        final ViewManager viewManager = this.surface.getViewManager ();
        for (int i = 0; i < 8; i++)
        {
            final Integer commandID = Integer.valueOf (Commands.COMMAND_ROW_SELECT_1.intValue () + i);
            viewManager.registerTriggerCommand (commandID, new SelectCommand (i, this.model, this.surface));
            this.surface.assignTriggerCommand (MCUControlSurface.MCU_FADER_TOUCH1 + i, commandID);
            this.surface.assignTriggerCommand (MCUControlSurface.MCU_SELECT1 + i, commandID);

            this.addTriggerCommand (Integer.valueOf (Commands.COMMAND_ROW1_1.intValue () + i), MCUControlSurface.MCU_VSELECT1 + i, new ButtonRowModeCommand<> (0, i, this.model, this.surface));
            this.addTriggerCommand (Integer.valueOf (Commands.COMMAND_ROW2_1.intValue () + i), MCUControlSurface.MCU_ARM1 + i, new ButtonRowModeCommand<> (1, i, this.model, this.surface));
            this.addTriggerCommand (Integer.valueOf (Commands.COMMAND_ROW3_1.intValue () + i), MCUControlSurface.MCU_SOLO1 + i, new ButtonRowModeCommand<> (2, i, this.model, this.surface));
            this.addTriggerCommand (Integer.valueOf (Commands.COMMAND_ROW4_1.intValue () + i), MCUControlSurface.MCU_MUTE1 + i, new ButtonRowModeCommand<> (3, i, this.model, this.surface));
        }
        this.addTriggerCommand (Commands.COMMAND_MASTERTRACK, MCUControlSurface.MCU_FADER_MASTER, new SelectCommand (8, this.model, this.surface));

        // Additional commands for footcontrollers
        viewManager.registerTriggerCommand (Commands.COMMAND_NEW, new NewCommand<> (this.model, this.surface));
        viewManager.registerTriggerCommand (Commands.COMMAND_TAP_TEMPO, new TapTempoCommand<> (this.model, this.surface));

        viewManager.registerPitchbendCommand (new PitchbendVolumeCommand (this.model, this.surface));
    }


    /** {@inheritDoc} */
    @Override
    protected void registerContinuousCommands ()
    {
        final ViewManager viewManager = this.surface.getViewManager ();
        for (int i = 0; i < 8; i++)
        {
            final Integer commandID = Integer.valueOf (Commands.CONT_COMMAND_KNOB1.intValue () + i);
            viewManager.registerContinuousCommand (commandID, new KnobRowModeCommand<> (i, this.model, this.surface));
            this.surface.assignContinuousCommand (MCUControlSurface.MCU_CC_VPOT1 + i, 1, commandID);
        }

        viewManager.registerContinuousCommand (Commands.CONT_COMMAND_PLAY_POSITION, new PlayPositionTempoCommand (this.model, this.surface));
        this.surface.assignContinuousCommand (MCUControlSurface.MCU_CC_JOG, 1, Commands.CONT_COMMAND_PLAY_POSITION);
    }


    /** {@inheritDoc} */
    @Override
    protected void startup ()
    {
        this.surface.switchVuMode (MCUControlSurface.VUMODE_LED);

        this.getHost ().scheduleTask ( () -> {
            this.surface.getViewManager ().setActiveView (Views.VIEW_CONTROL);
            this.surface.getModeManager ().setActiveMode (Modes.MODE_TRACK);
        }, 200);
    }


    @SuppressWarnings("unchecked")
    private void updateButtons ()
    {
        this.updateVUandFaders ();
        this.updateSegmentDisplay ();

        // Set button states
        final TransportProxy t = this.model.getTransport ();
        final boolean isShift = this.surface.isShiftPressed ();
        final boolean isFlipRecord = this.configuration.isFlipRecord ();
        final boolean isRecordShifted = isShift && !isFlipRecord || !isShift && isFlipRecord;

        final View view = this.surface.getViewManager ().getView (Views.VIEW_CONTROL);
        this.surface.updateButton (MCUControlSurface.MCU_REWIND, ((WindCommand<MCUControlSurface, MCUConfiguration>) view.getTriggerCommand (Commands.COMMAND_REWIND)).isRewinding () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_FORWARD, ((WindCommand<MCUControlSurface, MCUConfiguration>) view.getTriggerCommand (Commands.COMMAND_FORWARD)).isForwarding () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_REPEAT, t.isLoop () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_STOP, !t.isPlaying () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_PLAY, t.isPlaying () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_RECORD, isRecordShifted ? t.isLauncherOverdub () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF : t.isRecording () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);

        this.surface.updateButton (MCUControlSurface.MCU_NAME_VALUE, this.surface.getConfiguration ().isDisplayTrackNames () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_ZOOM, this.surface.getConfiguration ().isZoomState () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_SCRUB, this.surface.getModeManager ().isActiveMode (Modes.MODE_DEVICE_PARAMS) ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);

        this.surface.updateButton (MCUControlSurface.MCU_MIDI_TRACKS, MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_INPUTS, MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_AUDIO_TRACKS, this.model.getCursorDevice ().isWindowOpen () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_AUDIO_INSTR, MCU_BUTTON_STATE_OFF);

        this.surface.updateButton (MCUControlSurface.MCU_CLICK, (isShift ? t.isMetronomeTicksOn () : t.isMetronomeOn ()) ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_SOLO, this.model.getGroove ().getParameters ()[0].getValue () > 0 ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_REPLACE, (isShift ? t.isLauncherOverdub () : t.isArrangerOverdub ()) ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_FLIP, this.model.isEffectTrackBankActive () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);

        this.surface.updateButton (MCUControlSurface.MCU_SMPTE_BEATS, this.configuration.isDisplayTicks () ? MCU_BUTTON_STATE_OFF : MCU_BUTTON_STATE_ON);
    }


    private void updateSegmentDisplay ()
    {
        if (!this.configuration.hasSegmentDisplay ())
            return;

        final TransportProxy t = this.model.getTransport ();
        String positionText = t.getPositionText ();
        if (!this.configuration.isDisplayTicks ())
        {
            final String tempoStr = t.formatTempoNoFraction (t.getTempo ());
            final int pos = positionText.lastIndexOf (':');
            positionText = positionText.substring (0, pos + 1) + tempoStr;
        }

        this.surface.getSegmentDisplay ().setTransportPositionDisplay (positionText);
    }


    private void updateVUandFaders ()
    {
        final MidiOutput output = this.surface.getOutput ();
        final double upperBound = this.valueChanger.getUpperBound ();

        final boolean enableVUMeters = this.configuration.isEnableVUMeters ();
        final boolean hasMotorFaders = this.configuration.hasMotorFaders ();

        final AbstractTrackBankProxy tb = this.model.getCurrentTrackBank ();
        for (int i = 0; i < 8; i++)
        {
            final TrackData track = tb.getTrack (i);

            // Update VU LEDs of channel
            if (enableVUMeters)
            {
                final int vu = track.getVu ();
                if (vu != this.vuValues[i])
                {
                    this.vuValues[i] = vu;
                    final int scaledValue = (int) Math.round (vu * 12 / upperBound);
                    output.sendChannelAftertouch (0x10 * i + scaledValue, 0);
                }
            }

            // Update motor fader of channel
            if (hasMotorFaders)
            {
                final int volume = track.getVolume ();
                if (volume != this.faderValues[i])
                {
                    this.faderValues[i] = volume;
                    output.sendPitchbend (i, volume % 127, volume / 127);
                }
            }
        }

        final MasterTrackProxy masterTrack = this.model.getMasterTrack ();

        // Stereo VU of master channel
        if (enableVUMeters)
        {
            int vu = masterTrack.getVuLeft ();
            if (vu != this.vuValues[8])
            {
                this.vuValues[8] = vu;
                final int scaledValue = (int) Math.round (vu * 12 / upperBound);
                output.sendChannelAftertouch (1, scaledValue, 0);
            }

            vu = masterTrack.getVuRight ();
            if (vu != this.vuValues[9])
            {
                this.vuValues[9] = vu;
                final int scaledValue = (int) Math.round (vu * 12 / upperBound);
                output.sendChannelAftertouch (1, 0x10 + scaledValue, 0);
            }
        }

        // Update motor fader of master channel
        if (hasMotorFaders)
        {
            final int volume = masterTrack.getVolume ();
            if (volume != this.faderValues[8])
            {
                this.faderValues[8] = volume;
                output.sendPitchbend (8, volume % 127, volume / 127);
            }
        }
    }


    private void updateMode (final Integer mode)
    {
        if (mode == null)
            return;

        this.updateIndication (mode);

        final boolean isTrackOn = Modes.MODE_TRACK.equals (mode) || Modes.MODE_VOLUME.equals (mode);
        final boolean isPanOn = Modes.MODE_PAN.equals (mode);
        final boolean isSendOn = mode.intValue () >= Modes.MODE_SEND1.intValue () && mode.intValue () <= Modes.MODE_SEND8.intValue ();
        final boolean isDeviceOn = Modes.MODE_DEVICE_PARAMS.equals (mode);

        this.surface.updateButton (MCUControlSurface.MCU_MODE_IO, isTrackOn ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_MODE_PAN, isPanOn ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_MODE_SENDS, isSendOn ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_MODE_PLUGIN, isDeviceOn ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_USER, Modes.MODE_BROWSER.equals (mode) ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);

        final TransportProxy transport = this.model.getTransport ();
        final String automationWriteMode = transport.getAutomationWriteMode ();
        final boolean writingArrangerAutomation = transport.isWritingArrangerAutomation ();

        this.surface.updateButton (MCUControlSurface.MCU_F6, transport.isPunchInEnabled () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_F7, transport.isPunchOutEnabled () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);

        this.surface.updateButton (MCUControlSurface.MCU_READ, !writingArrangerAutomation ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_WRITE, writingArrangerAutomation && TransportProxy.AUTOMATION_MODES_VALUES[2].equals (automationWriteMode) ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_TRIM, transport.isWritingClipLauncherAutomation () ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_TOUCH, writingArrangerAutomation && TransportProxy.AUTOMATION_MODES_VALUES[1].equals (automationWriteMode) ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
        this.surface.updateButton (MCUControlSurface.MCU_LATCH, writingArrangerAutomation && TransportProxy.AUTOMATION_MODES_VALUES[0].equals (automationWriteMode) ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF);
    }


    private void updateIndication (final Integer mode)
    {
        final TrackBankProxy tb = this.model.getTrackBank ();
        final EffectTrackBankProxy tbe = this.model.getEffectTrackBank ();
        final boolean isEffect = this.model.isEffectTrackBankActive ();
        final boolean isPan = Modes.MODE_PAN.equals (mode);
        final boolean isTrack = Modes.MODE_TRACK.equals (mode);

        tb.setIndication (!isEffect);
        tbe.setIndication (isEffect);

        final CursorDeviceProxy cursorDevice = this.model.getCursorDevice ();
        final TrackData selectedTrack = tb.getSelectedTrack ();
        for (int i = 0; i < tb.getNumTracks (); i++)
        {
            final boolean hasTrackSel = selectedTrack != null && selectedTrack.getIndex () == i && Modes.MODE_TRACK.equals (mode);
            tb.setVolumeIndication (i, !isEffect && (isTrack || hasTrackSel));
            tb.setPanIndication (i, !isEffect && (isPan || hasTrackSel));

            for (int j = 0; j < tb.getNumSends (); j++)
                tb.setSendIndication (i, j, !isEffect && (mode.intValue () - Modes.MODE_SEND1.intValue () == j || hasTrackSel));

            tbe.setVolumeIndication (i, isEffect);
            tbe.setPanIndication (i, isEffect && isPan);

            cursorDevice.getParameter (i).setIndication (true);
        }
    }


    /**
     * Handle a track selection change.
     *
     * @param index The index of the track
     * @param isSelected Has the track been selected?
     */
    private void handleTrackChange (final int index, final boolean isSelected)
    {
        if (!isSelected)
            return;

        final ModeManager modeManager = this.surface.getModeManager ();
        if (modeManager.isActiveMode (Modes.MODE_MASTER))
            modeManager.setActiveMode (Modes.MODE_TRACK);
    }
}
