package apphhzp.lib.hotspot.runtime;

public class ExternalVFrame extends VFrame{
    protected ExternalVFrame(Frame fr, RegisterMap reg_map, JavaThread thread){
        super(fr, reg_map, thread);
    }
}
