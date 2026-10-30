package com.logatron.agent.application;

import com.logatron.agent.configuration.ShipperManagementProperties;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Fixed executable and argument vector; no shell, remote commands, or client executable input. */
@Component
public class CollectorConfigurationValidator {
    private final ShipperManagementProperties settings;
    private final Semaphore slots=new Semaphore(2);
    public CollectorConfigurationValidator(ShipperManagementProperties settings){this.settings=settings;}
    public Result validate(String yaml){
        if(settings.validatorBinary().isBlank())return new Result("UNAVAILABLE","Install contrib 0.135.0 and configure logatron.shipper.validator-binary.");
        if(!slots.tryAcquire())return new Result("UNAVAILABLE","Collector validation is busy. Retry shortly.");
        Path directory=null;
        try{
            directory=Files.createTempDirectory("logatron-validate-");
            String version=run(directory,"--version");
            if(!version.matches("(?s).*otelcol-contrib.*\\b0\\.135\\.0\\b.*"))return new Result("INVALID","Validator must be otelcol-contrib 0.135.0.");
            Path file=directory.resolve("config.yml");Files.writeString(file,yaml,StandardCharsets.UTF_8);
            run(directory,"validate","--config="+file.toAbsolutePath());
            return new Result("VALID","Validated with otelcol-contrib 0.135.0. Target validation is still required before activation.");
        }catch(Exception e){return new Result("INVALID","Collector validation failed. Check the configured binary, target certificate paths and generated configuration. "+safe(e));}
        finally{if(directory!=null)try(var paths=Files.walk(directory)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}catch(Exception ignored){}slots.release();}
    }
    private String run(Path dir,String...arguments)throws Exception{
        var command=new java.util.ArrayList<String>();command.add(settings.validatorBinary());command.addAll(java.util.List.of(arguments));
        Path output=dir.resolve("validation.log");Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try{if(!process.waitFor(15,TimeUnit.SECONDS))throw new IllegalStateException("Timed out after 15 seconds.");
            // Never return Collector diagnostics to clients: diagnostics can contain paths or environment secrets.
            if(process.exitValue()!=0)throw new IllegalStateException("Collector rejected the configuration (exit "+process.exitValue()+").");
            try(var input=Files.newInputStream(output)){return new String(input.readNBytes(8192),StandardCharsets.UTF_8);}
        }finally{if(process.isAlive()){process.destroyForcibly();process.waitFor(2,TimeUnit.SECONDS);}}
    }
    private static String safe(Exception e){return e instanceof IllegalStateException?e.getMessage():"Validation process could not run.";}
    public record Result(String status,String message){}
}
