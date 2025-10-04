package apphhzp.lib.hotspot.runtime;

public class EntryVFrame extends ExternalVFrame{
    public boolean is_entry_frame(){ return true; }
    protected EntryVFrame(Frame fr, RegisterMap reg_map, JavaThread thread){
        super(fr, reg_map, thread);
    }
}
