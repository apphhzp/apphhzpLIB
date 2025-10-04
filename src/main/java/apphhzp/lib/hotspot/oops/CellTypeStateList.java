package apphhzp.lib.hotspot.oops;


import java.util.ArrayList;
import java.util.List;

public class CellTypeStateList {
    public CellTypeStateList(int size) {
        list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i, CellTypeState.make_bottom());
        }
    }

    public int size() {
        return list.size();
    }

    public CellTypeState get(int i) {
        return list.get(i);
    }
    public void set(int i, CellTypeState state){
        list.set(i,state);
    }

    public CellTypeStateList subList(int fromIndex, int toIndex) {
        return new CellTypeStateList(list.subList(fromIndex, toIndex));
    }

    //----------------------------------------------------------------------
    private List<CellTypeState> list;
    private CellTypeStateList(List<CellTypeState> list) {
        this.list = list;
    }
}
