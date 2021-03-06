package io.jenkins.plugins.pipeline.restful.api;

import com.cloudbees.workflow.util.ServeJson;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import jenkins.model.identity.IdentityRootAction;
import jenkins.slaves.JnlpSlaveAgentProtocol;
import org.acegisecurity.AccessDeniedException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
@Restricted(NoExternalUse.class)
public class InstanceAPI implements RootAction {
    private static final Logger LOGGER = Logger.getLogger(InstanceAPI.class.getName());

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Jenkins instance API";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "instance";
    }

    @ServeJson
    public JenkinsInstance doIndex() {
        IdentityRootAction identityAction = new IdentityRootAction();

        JenkinsInstance jenkinsInstance = new JenkinsInstance(identityAction.getFingerprint(), identityAction.getPublicKey());
        jenkinsInstance.setSystemMessage(Jenkins.get().getSystemMessage());

        return jenkinsInstance;
    }

    @ServeJson
    public Properties doSystemProperties() {
        try {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            return System.getProperties();
        } catch (AccessDeniedException e) {
            LOGGER.log(Level.SEVERE, "no permission to get system properties", e);
        }
        return new Properties();
    }

    @ServeJson
    public Map<String, String> doSystemEnv() {
        try {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            return System.getenv();
        } catch (AccessDeniedException e) {
            LOGGER.log(Level.SEVERE, "no permission to get system environment", e);
        }
        return new HashMap<>();
    }

    @RequirePOST
    public HttpResponse doUpdateMessage(StaplerRequest req) {
        String message = req.getParameter("message");

        try {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            Jenkins.get().setSystemMessage(message);
        } catch (IOException | AccessDeniedException e) {
            LOGGER.log(Level.SEVERE, "cannot set system message of Jenkins", e);
            return HttpResponses.errorJSON(e.getMessage());
        }

        return HttpResponses.ok();
    }

    @RequirePOST
    public HttpResponse doAgentSecret(@QueryParameter String name) {
        try {
            return HttpResponses.text(JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(name));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "cannot get the slave secret", e);
            return HttpResponses.errorJSON(e.getMessage());
        }
    }
}

class JenkinsInstance {
    private String fingerprint;
    private String publicKey;
    private String systemMessage;

    public JenkinsInstance(String fingerprint, String publicKey) {
        this.fingerprint = fingerprint;
        this.publicKey = publicKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }
}
