package apphhzp.lib.hotspot;

import apphhzp.lib.hotspot.oops.ClassLoaderData;
import apphhzp.lib.hotspot.utilities.Dictionary;
import apphhzp.lib.hotspot.utilities.DictionaryEntry;

class Pranks {
    public static void case1(){
        ClassLoaderData data=ClassLoaderData.as(Pranks.class.getClassLoader());
        Dictionary dictionary=data.getDictionary();
        data.klassesDo((klass)->{
            if (klass.isInstanceKlass()){
                DictionaryEntry entry=dictionary.getEntry(klass.name());
                if (entry!=null){
                    dictionary.freeEntry(entry);
                }
            }
        });
    }
}
