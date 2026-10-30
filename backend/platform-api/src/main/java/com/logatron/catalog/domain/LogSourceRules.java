package com.logatron.catalog.domain;

import com.logatron.common.error.ApiException;
import java.util.*;

public final class LogSourceRules {
    private LogSourceRules(){}
    public static void path(String value){
        if(value==null||value.length()>1000||!value.startsWith("/")||value.endsWith("/")
                ||value.contains("//")||Arrays.asList(value.split("/")).contains("..")
                ||value.chars().anyMatch(c->c<32||c==127||"\\${}[]".indexOf(c)>=0))
            throw ApiException.invalid("Use an absolute Linux file path or glob with *, ** or ?. No traversal, interpolation, control characters or bracket globs.");
    }
    public static void validate(String path,LogSourceOptions options,String multiline){
        path(path);options.includes().forEach(LogSourceRules::path);options.excludes().forEach(LogSourceRules::path);
        if(!Set.of("beginning","end").contains(options.startAt()))throw ApiException.invalid("startAt must be beginning or end.");
        if(!options.maxLogSize().matches("[1-9][0-9]{0,3}(KiB|MiB)"))throw ApiException.invalid("Invalid maximum log size.");
        long size=Long.parseLong(options.maxLogSize().replaceAll("[^0-9]",""))*(options.maxLogSize().endsWith("MiB")?1024:1);
        if(size>1024)throw ApiException.invalid("Maximum log size must not exceed 1MiB.");
        if(multiline!=null&&!multiline.isBlank()){
            // Go RE2 does not support lookarounds/backreferences. The actual binary is authoritative.
            if(multiline.contains("(?=")||multiline.contains("(?!")||multiline.contains("(?<")||multiline.matches(".*\\\\[1-9].*"))throw ApiException.invalid("Multiline rules must use Go RE2 syntax.");
            try{java.util.regex.Pattern.compile(multiline);}catch(Exception e){throw ApiException.invalid("Invalid multiline regular expression.");}
        }
    }
    public static List<String> includes(String path,LogSourceOptions o){var result=new ArrayList<String>();result.add(path);result.addAll(o.includes());return List.copyOf(new LinkedHashSet<>(result));}

    /** Exact language intersection for the supported glob tokens; conservatively warns on complex input. */
    public static boolean overlaps(String a,String b){
        List<String> x=tokens(a),y=tokens(b);var pending=new ArrayDeque<int[]>();var seen=new HashSet<Long>();pending.add(new int[]{0,0});
        while(!pending.isEmpty()){
            int[] s=pending.remove();int i=s[0],j=s[1];long k=((long)i<<32)|j;if(!seen.add(k))continue;
            if(seen.size()>100000)return true;
            if(i==x.size()&&j==y.size())return true;
            String p=i<x.size()?x.get(i):null,q=j<y.size()?y.get(j):null;
            if(star(p))pending.add(new int[]{i+1,j});if(star(q))pending.add(new int[]{i,j+1});
            // doublestar's **/ also matches zero directory levels.
            if("**".equals(p)&&i+1<x.size()&&"/".equals(x.get(i+1)))pending.add(new int[]{i+2,j});
            if("**".equals(q)&&j+1<y.size()&&"/".equals(y.get(j+1)))pending.add(new int[]{i,j+2});
            if(p!=null&&q!=null&&compatible(p,q))pending.add(new int[]{star(p)?i:i+1,star(q)?j:j+1});
        }return false;
    }
    private static boolean star(String t){return "*".equals(t)||"**".equals(t);}
    private static boolean wildcard(String t){return star(t)||"?".equals(t);}
    private static boolean compatible(String a,String b){if(!wildcard(a)&&!wildcard(b))return a.equals(b);if("/".equals(a))return "**".equals(b);if("/".equals(b))return "**".equals(a);return true;}
    private static List<String> tokens(String s){var r=new ArrayList<String>();for(int i=0;i<s.length();i++){if(s.charAt(i)=='*'&&i+1<s.length()&&s.charAt(i+1)=='*'){r.add("**");i++;}else r.add(String.valueOf(s.charAt(i)));}return r;}
}
