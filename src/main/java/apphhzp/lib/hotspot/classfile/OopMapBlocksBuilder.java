package apphhzp.lib.hotspot.classfile;

import apphhzp.lib.hotspot.oops.OopMapBlock;

import java.util.Arrays;

public class OopMapBlocksBuilder{
    public OopMapBlock[] _nonstatic_oop_maps;
    public int _nonstatic_oop_map_count;
    public int _max_nonstatic_oop_maps;

    public OopMapBlocksBuilder(int  max_blocks){
        _max_nonstatic_oop_maps = max_blocks;
        _nonstatic_oop_map_count = 0;
        if (max_blocks == 0) {
            _nonstatic_oop_maps = null;
        } else {
            _nonstatic_oop_maps =new OopMapBlock[_max_nonstatic_oop_maps];
            for (int i=0;i<max_blocks;i++) {
                _nonstatic_oop_maps[i]=new OopMapBlock(0,0);
            }
        }
    }
    public OopMapBlock last_oop_map() {
        if (_nonstatic_oop_map_count<=0){
            throw new IllegalStateException("Has no oop maps");
        }
        return _nonstatic_oop_maps[(_nonstatic_oop_map_count - 1)];
    }

    public void initialize_inherited_blocks(OopMapBlock[] blocks, int nof_blocks) {
        if (!(nof_blocks>0 && _nonstatic_oop_map_count == 0 &&
                nof_blocks <= _max_nonstatic_oop_maps)){
            throw new IllegalArgumentException("invariant");
        }
        for (int i=0;i<nof_blocks;i++) {
            _nonstatic_oop_maps[i]=new OopMapBlock(blocks[i].offset(),blocks[i].count());
        }
        //memcpy(_nonstatic_oop_maps, blocks, sizeof(OopMapBlock) * nof_blocks);
        _nonstatic_oop_map_count += nof_blocks;
    }

    public void add(int offset, int count) {
        if (_nonstatic_oop_map_count == 0) {
            _nonstatic_oop_map_count++;
        }
        OopMapBlock nonstatic_oop_map = last_oop_map();
        if (nonstatic_oop_map.count() == 0) {  // Unused map, set it up
            nonstatic_oop_map.set_offset(offset);
            nonstatic_oop_map.set_count(count);
        } else if (nonstatic_oop_map.is_contiguous(offset)) { // contiguous, add
            nonstatic_oop_map.increment_count(count);
        } else { // Need a new one...
            _nonstatic_oop_map_count++;
            if (_nonstatic_oop_map_count > _max_nonstatic_oop_maps){
                throw new IllegalStateException("range check");
            }
            nonstatic_oop_map = last_oop_map();
            nonstatic_oop_map.set_offset(offset);
            nonstatic_oop_map.set_count(count);
        }
    }

    // general purpose copy, e.g. into allocated instanceKlass
    public void copy(OopMapBlock[] dst) {
        if (_nonstatic_oop_map_count != 0) {
            for (int i=0;i<_nonstatic_oop_map_count;i++){
                dst[i]=new OopMapBlock(_nonstatic_oop_maps[i].offset(),_nonstatic_oop_maps[i].count());
            }
        }
    }

    public void compact() {
        if (_nonstatic_oop_map_count <= 1) {
            return;
        }
        /*
         * Since field layout sneeks in oops before values, we will be able to condense
         * blocks. There is potential to compact between super, own refs and values
         * containing refs.
         *
         * Currently compaction is slightly limited due to values being 8 byte aligned.
         * This may well change: FixMe if it doesn't, the code below is fairly general purpose
         * and maybe it doesn't need to be.
         */
//        qsort(_nonstatic_oop_maps, _nonstatic_oop_map_count, sizeof(OopMapBlock),
//                (_sort_Fn)OopMapBlock::compare_offset);
        Arrays.sort(_nonstatic_oop_maps,0, _nonstatic_oop_map_count, OopMapBlock::compare_offset);
        if (_nonstatic_oop_map_count < 2) {
            return;
        }

        // Make a temp copy, and iterate through and copy back into the original
        OopMapBlock[] oop_maps_copy =new OopMapBlock[_nonstatic_oop_map_count];
        int copy_index=0;
                //NEW_RESOURCE_ARRAY(OopMapBlock, _nonstatic_oop_map_count);
        //OopMapBlock oop_maps_copy_end = oop_maps_copy + _nonstatic_oop_map_count;
        int oop_maps_copy_end=_nonstatic_oop_map_count;
        copy(oop_maps_copy);
        OopMapBlock[] nonstatic_oop_map = _nonstatic_oop_maps;
        int index=0;
        int new_count = 1;
        //oop_maps_copy++;
        copy_index++;
        while(copy_index < oop_maps_copy_end) {
            if (!(nonstatic_oop_map[index].offset() < oop_maps_copy[copy_index].offset())){
                throw new RuntimeException("invariant");
            }
            if (nonstatic_oop_map[index].is_contiguous(oop_maps_copy[copy_index].offset())) {
                nonstatic_oop_map[index].increment_count(oop_maps_copy[copy_index].count());
            } else {
                index++;
                new_count++;
                nonstatic_oop_map[index].set_offset(oop_maps_copy[copy_index].offset());
                nonstatic_oop_map[index].set_count(oop_maps_copy[copy_index].count());
            }
            //oop_maps_copy++;
            copy_index++;
        }
        if (new_count>_nonstatic_oop_map_count) {
            throw new IllegalStateException("end up with more maps after compact() ?");
        }
        _nonstatic_oop_map_count = new_count;
    }

//    public void print_on(PrintStream st) {
//        st.println("  OopMapBlocks: %3d  /%3d", _nonstatic_oop_map_count, _max_nonstatic_oop_maps);
//        if (_nonstatic_oop_map_count > 0) {
//            OopMapBlock* map = _nonstatic_oop_maps;
//            OopMapBlock* last_map = last_oop_map();
//            assert(map <= last_map, "Last less than first");
//            while (map <= last_map) {
//                st->print_cr("    Offset: %3d  -%3d Count: %3d", map->offset(),
//                        map->offset() + map->offset_span() - heapOopSize, map->count());
//                map++;
//            }
//        }
//    }
}
