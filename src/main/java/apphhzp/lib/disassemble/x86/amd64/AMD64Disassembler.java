package apphhzp.lib.disassemble.x86.amd64;

import apphhzp.lib.PlatformInfo;
import apphhzp.lib.disassemble.x86.amd64.instruction.opcode.*;
import apphhzp.lib.disassemble.x86.amd64.instruction.prefix.RexPrefix;

import java.util.ArrayList;
import java.util.List;

import static apphhzp.lib.ClassHelperSpecial.unsafe;

public class AMD64Disassembler {

    private interface CodeGetter{
        int get(int idx);
        int getWord(int idx);
        int length();
    }

    public static List<AMD64Opcode> disassemble(long addr, int len){
        return disassemble(new CodeGetter(){
            @Override
            public int get(int idx) {
                return (unsafe.getByte(addr+idx)&0xff);
            }

            @Override
            public int length() {
                return len;
            }

            @Override
            public int getWord(int idx) {
                return unsafe.getShort(addr+idx)&0xffff;
            }
        });
    }
    private static List<AMD64Opcode> disassemble(CodeGetter codeGetter){
        if (true){
            throw new UnsupportedOperationException("Oh! Shit! I can't finish it!");
        }
        if (!PlatformInfo.isX86_64()){
            throw new RuntimeException("Wrong CPU architecture");
        }
        List<AMD64Opcode> re = new ArrayList<>();
        RexPrefix rex=null;
        boolean hasLockPrefix,hasSizeOverridePrefix,hasAddressSizeOverridePrefix;
        for (int i=0;i<codeGetter.length();){
            int b= codeGetter.get(i);
            AMD64Opcode opcode=null;
            hasLockPrefix=hasSizeOverridePrefix=false;
            if (b==0xf0){
                hasLockPrefix=true;
                b=codeGetter.get(++i);
            }
            if (b==0x66){
                hasSizeOverridePrefix=true;
                b=codeGetter.get(++i);
            }
            if (b==0x67){
                hasAddressSizeOverridePrefix=true;
                b=codeGetter.get(++i);
            }
            rex=null;
            if ((b>>4)==4){//REX Prefix
                rex=new RexPrefix((byte)b);
                b=codeGetter.get(++i);
            }

            if(b==0x90){
                opcode=new NOP();
            }else if (b==0xcc){
                opcode=new INT();
            }else if (b==0xcd){
                opcode=new INT(codeGetter.get(i+1));
            }else if (b==0xce){
                opcode=new INTO();
            }else if (b==0xcf){
                opcode=new IRET();
            }else if (b==0xc3){
                opcode=new NearRET();
            }else if (b==0xc2){
                opcode=new NearRET(codeGetter.getWord(i+1));
            }else if (b>=0x58&&b<=0x5f){
                opcode=new POP(POP.code2reg((byte)(b&0xff),rex!=null&&rex.b()));
            }else if (b>=0x50&&b<=0x57){
                opcode=new PUSH(PUSH.code2reg((byte)(b&0xff),rex!=null&&rex.b()));
            }

            if (opcode==null){
                throw new RuntimeException();
            }
            re.add(opcode);
            i+=opcode.size();
        }
        return re;
    }
}
