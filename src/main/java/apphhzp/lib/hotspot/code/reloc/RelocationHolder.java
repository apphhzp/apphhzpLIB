package apphhzp.lib.hotspot.code.reloc;

import apphhzp.lib.hotspot.util.RawCType;

/** Holder for flyweight relocation objects.
 * Although the flyweight subclasses are of varying sizes,
 * the holder is "one size fits all".*/
public class RelocationHolder {
    // this preallocated memory must accommodate all subclasses of Relocation
    // (this number is assertion-checked in Relocation::operator new)
    private static final int  _relocbuf_size = 5 ;
    private @RawCType("void*")Relocation[] _relocbuf =new Relocation[_relocbuf_size];


    public @RawCType("Relocation*") Relocation[] reloc(){
        return _relocbuf;
    }
    public @RawCType("relocInfo::relocType") int type(){
        return reloc()[0].type();
    }

    // Add a constant offset to a relocation.  Helper for class Address.
    public RelocationHolder plus(int offset){
        if (offset != 0) {
            switch (type()) {
                case RelocInfo.relocType.none:
                    break;
                case RelocInfo.relocType.oop_type:
                {
                    //TODO
                    throw new UnsupportedOperationException("TODO");
//                    oop_Relocation* r = (oop_Relocation*)reloc();
//                    return oop_Relocation::spec(r->oop_index(), r->offset() + offset);
                }
                case RelocInfo.relocType.metadata_type:
                {
                    //TODO
                    throw new UnsupportedOperationException("TODO");
//                    metadata_Relocation* r = (metadata_Relocation*)reloc();
//                    return metadata_Relocation::spec(r->metadata_index(), r->offset() + offset);
                }
                default:
                    throw new RuntimeException("ShouldNotReachHere()");
            }
        }
        return this.clone();
    }
    // initializes type to none
    public RelocationHolder(){
    }

    public RelocationHolder(Relocation[] r){ // make a copy
        // wordwise copy from r (ok if it copies garbage after r)
        for (int i = 0; i < _relocbuf_size; i++) {
            _relocbuf[i] = r[i];
        }
    }

    public static final RelocationHolder none=new RelocationHolder();

    @Override
    public RelocationHolder clone(){
        RelocationHolder re=new RelocationHolder();
        re._relocbuf = re._relocbuf.clone();
        return re;
    }
}
