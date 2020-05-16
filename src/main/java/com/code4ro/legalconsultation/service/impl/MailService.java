package com.code4ro.legalconsultation.service.impl;

import com.code4ro.legalconsultation.common.exceptions.LegalValidationException;
import com.code4ro.legalconsultation.model.persistence.DocumentMetadata;
import com.code4ro.legalconsultation.model.persistence.User;
import com.code4ro.legalconsultation.service.api.MailApi;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Profile("production")
public class MailService implements MailApi{
    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    @Value("${app.email.signupurl}")
    private String signupUrl;

    @Value("${app.email.documenturl}")
    private String documentUrl;

    @Value("${spring.mvc.locale}")
    private String configuredLocale;

    private final JavaMailSender mailSender;
    private final I18nService i18nService;
    private final Configuration freemarkerConfig;

    @Autowired
    public MailService(final JavaMailSender mailSender,
                       final I18nService i18nService,
                       final Configuration freemarkerConfig) {
        this.mailSender = mailSender;
        this.i18nService = i18nService;
        this.freemarkerConfig = freemarkerConfig;
    }

    @Override
    public void sendRegisterMail(final List<User> users) throws LegalValidationException {
        final List<String> failedEmails = new ArrayList<>();

        String translatedSubject = i18nService.translate("register.User.confirmation.subject");
        String registerTemplate = getRegisterTemplate();
        users.forEach(user ->
            buildAndSendEmail(translatedSubject, registerTemplate, getRegisterModel(user), user.getEmail())
                    .ifPresent(failedEmails::add)
        );

        if (!failedEmails.isEmpty()) {
            throw new LegalValidationException("user.Email.send.failed", failedEmails, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void sendDocumentAssignedEmail(final DocumentMetadata documentMetadata, final List<User> users) {
        final List<String> failedEmails = new ArrayList<>();

        String translatedSubject = i18nService.translate("email.documentAssigned.subject");
        String documentAssignedTemplate = getDocumentAssignedTemplate();
        users.forEach(user ->
                buildAndSendEmail(translatedSubject, documentAssignedTemplate, getDocumentAssignedModel(documentMetadata, user), user.getEmail())
                    .ifPresent(failedEmails::add)
        );

        if (!failedEmails.isEmpty()) {
            throw new LegalValidationException("user.Email.send.failed", failedEmails, HttpStatus.BAD_REQUEST);
        }
    }

    private Optional<String> buildAndSendEmail(String subject, String templateName, Map<String, String> model, String userEmail){
        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message);
            //TODO: add From here from previous #121 issue
            helper.setTo(userEmail);
            helper.setSubject(subject);
            final Template template = freemarkerConfig.getTemplate(templateName);
            final String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            helper.setText(content, true);
            mailSender.send(message);
            return Optional.empty();
        } catch (final Exception e) {
            LOG.error("Problem preparing or sending email to user with address {}", userEmail, e);
            return Optional.of(userEmail);
        }
    }

    private Map<String, String> getDocumentAssignedModel(final DocumentMetadata documentMetadata, final User user) {
        return Map.of(
                "username", getUserName(user),
                "documenturl", getDocumentUrl(documentMetadata)
        );
    }

    private String getDocumentUrl(DocumentMetadata documentMetadata) {
        return documentUrl + "/" + documentMetadata.getId();
    }

    private String getDocumentAssignedTemplate() {
        return "document-assigned-email-" + configuredLocale + ".ftl";
    }

    private String getRegisterTemplate() {
        return "register-email-" + configuredLocale + ".ftl";
    }

    private Map<String, String> getRegisterModel(final User user) {
        return Map.of(
                "username", getUserName(user),
                "signupurl", getSignupUrl(user)
        );
    }

    private String getSignupUrl(final User user) {
        return signupUrl + '/' + user.getEmail();
    }

    private String getUserName(final User user) {
        final String USERNAME_SEPARATOR = " ";
        return Stream.of(user.getFirstName(), user.getLastName())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(USERNAME_SEPARATOR));
    }
}
