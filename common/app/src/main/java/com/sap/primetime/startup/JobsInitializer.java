package com.sap.primetime.startup;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.jobs.DBCleanUpJob;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.ConfigUtil;

public class JobsInitializer {
	private static Logger logger = LoggerFactory.getLogger(JobsInitializer.class);
	private static Scheduler scheduler;

	public static void shutDown() {
		if (scheduler != null) {
			try {
				scheduler.shutdown();
			} catch (SchedulerException e) {
				logger.error("Job scheduler could not be shut down.", e);
			}
		}
	}

	public static Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Schedules all background jobs
	 *
	 * Important: Group names must be equal to the job name currently for manually
	 * triggering jobs via REST
	 */
	public static void initScheduler() {
		try {
			// shutdown existing scheduler
			if (scheduler != null) {
				logger.info("Shutting down Quartz...");
				scheduler.shutdown(true);
				logger.info("Shutdown complete.");
			}

			SchedulerFactory sf = new StdSchedulerFactory();
			scheduler = sf.getScheduler();
			scheduler.start();

			JobDetail jobDetail = newJob(DBCleanUpJob.class).withIdentity("DBCleanUp", "DBCleanUp").build();
			String jobSchedule = ConfigUtil.getProperty(Consts.APP, Consts.PROP_CRON_DBCLEANUP);
			CronTrigger cronTrigger = newTrigger().withIdentity("cronTrigger", "DBCleanUp")
					.withSchedule(cronSchedule(jobSchedule)).build();
			scheduleJob(jobDetail, cronTrigger);
		} catch (SchedulerException e) {
			logger.error("Job scheduler could not be started.", e);
		}
	}

	private static void scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
		try {
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (ObjectAlreadyExistsException e) {
			scheduler.rescheduleJob(trigger.getKey(), trigger);
		}
	}
}
