package apphhzp.lib.hotspot.oops.method;

public class ExceptionTable{
    private final ExceptionTableElement[] _table;
    private final int  _length;
    public ExceptionTable(Method method) {
        if (ConstMethod.hasExceptionTable(method.constMethod().getFlags())){
            _table=method.constMethod().getExceptionTable();
            //noinspection DataFlowIssue
            _length=_table.length;
        }else {
            _table=null;
            _length=0;
        }
    }

    public int length() {
        return _length;
    }

    private void checkBounds(int idx){
        if (idx<0 || idx>=_length){
            throw new IndexOutOfBoundsException("out of bounds");
        }
    }

    public int start_pc(int idx) {
        checkBounds(idx);
        return _table[idx].start_pc();
    }

    public void set_start_pc(int idx,int value) {
        checkBounds(idx);
        _table[idx].set_start_pc(value);;
    }

    public int end_pc(int idx) {
        checkBounds(idx);
        return _table[idx].end_pc();
    }

    public void set_end_pc(int idx, int value) {
        checkBounds(idx);
        _table[idx].set_end_pc(value);
    }

    public int handler_pc(int idx) {
        checkBounds(idx);
        return _table[idx].handler_pc();
    }

    public void set_handler_pc(int idx, int value) {
        checkBounds(idx);
        _table[idx].set_handler_pc(value);
    }

    public int catch_type_index(int idx){
        checkBounds(idx);
        return _table[idx].catch_type_index();
    }

    public void set_catch_type_index(int idx, int value) {
        checkBounds(idx);
        _table[idx].set_catch_type_index(value);
    }
}
