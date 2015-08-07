package com.linkedin.thirdeye.reporting.api;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.linkedin.thirdeye.reporting.api.anomaly.AnomalyReportTable;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class ReportEmailSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportEmailSender.class);

  private List<Table> tables;
  private ScheduleSpec scheduleSpec;
  private ReportConfig reportConfig;
  private Map<String, AnomalyReportTable> anomalyReportTables;
  private String templatePath;

  public ReportEmailSender(List<Table> tables, ScheduleSpec scheduleSpec, ReportConfig reportConfig, Map<String, AnomalyReportTable> anomalyReportTables, String templatePath) {
    this.tables = tables;
    this.scheduleSpec = scheduleSpec;
    this.reportConfig = reportConfig;
    this.anomalyReportTables = anomalyReportTables;
    this.templatePath = templatePath;
  }

  public void emailReport()  {

    try {

      FileTemplateLoader ftl = new FileTemplateLoader(
          new File(templatePath));
      Configuration emailConfiguration = new Configuration();
      emailConfiguration.setTemplateLoader(ftl);
      Template emailReportTemplate = emailConfiguration.getTemplate(scheduleSpec.getEmailTemplate());

      //TODO: Use POJO with accessors to the keys instead of Map<String, Object>
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put(ReportConstants.REPORT_CONFIG_OBJECT, reportConfig);
      rootMap.put(ReportConstants.TABLES_OBJECT, tables);
      rootMap.put(ReportConstants.ANOMALY_TABLES_OBJECT, anomalyReportTables);

      Writer emailOutput = new StringWriter();
      emailReportTemplate.process(rootMap, emailOutput);

      Properties props = new Properties();
      props.setProperty(ReportConstants.MAIL_SMTP_HOST_KEY, ReportConstants.MAIL_SMTP_HOST_VALUE);
      Session session = Session.getDefaultInstance(props, null);

      Message emailReportMessage = new MimeMessage(session);
      for (String emailIdFrom : scheduleSpec.getEmailFrom().split(",")) {
        emailReportMessage.setFrom(new InternetAddress(emailIdFrom, scheduleSpec.getNameFrom()));
      }
      for (String emailIdTo : scheduleSpec.getEmailTo().split(",")) {
        emailReportMessage.addRecipient(Message.RecipientType.TO,
                         new InternetAddress(emailIdTo, scheduleSpec.getNameTo()));
      }
      emailReportMessage.setSubject(ReportConstants.REPORT_SUBJECT_PREFIX +
          " " + reportConfig.getCollection().toUpperCase() +
          " (" + reportConfig.getEndTimeString() +
          ") " + reportConfig.getName());
      emailReportMessage.setContent(emailOutput.toString(), "text/html");
      LOGGER.info("Sending email from {} to {}  ",
          scheduleSpec.getEmailFrom(), scheduleSpec.getEmailTo());

      Transport.send(emailReportMessage);

    } catch (Exception e) {
     e.printStackTrace();
    }
  }

}
