package apphhzp.lib.hotspot.compiler;

import apphhzp.lib.hotspot.stream.CompressedReadStream;

public class OopMapStream {
    private CompressedReadStream _stream;
    private int _size;
    private int _position;
    private boolean _valid_omv;
    private OopMapValue _omv=new OopMapValue();
    private void find_next(){
        if (_position++ < _size) {
            _omv.read_from(_stream);
            _valid_omv = true;
            return;
        }
        _valid_omv = false;
    }

//    public OopMapStream(OopMap oop_map) {
//        _stream = new CompressedReadStream(oop_map->write_stream()->buffer());
//        _size = oop_map->omv_count();
//        _position = 0;
//        _valid_omv = false;
//    }

    public OopMapStream(ImmutableOopMap oop_map) {
        _stream = new CompressedReadStream(oop_map.data_addr(),0);
        _size = oop_map.count();
        _position = 0;
        _valid_omv = false;
    }
    public boolean is_done()                        { if(!_valid_omv) { find_next(); } return !_valid_omv; }
    public void next()                           { find_next(); }
    public OopMapValue current()                 { return _omv; }
    public int stream_position(){return _stream.pos();}
}
