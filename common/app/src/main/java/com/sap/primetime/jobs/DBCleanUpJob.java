package com.sap.primetime.jobs;

import java.text.MessageFormat;
import java.util.List;

import org.joda.time.DateTime;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.dao.EventHistoryDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.entities.Screen;

@DisallowConcurrentExecution
public class DBCleanUpJob implements Job {
	private static final Logger logger = LoggerFactory.getLogger(DBCleanUpJob.class);

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		deleteOutdatedEvents();
		deleteOutdatedOnboardings();
	}

	private void deleteOutdatedEvents() {
		DateTime threshold = DateTime.now().minusDays(7);
		int deletedCount = new EventHistoryDAO().deleteAllBefore(threshold);
		if (deletedCount > 0) {
			logger.info(MessageFormat.format("Deleted {0} outdated events from before {1}.", deletedCount, threshold));
		}
	}

	private void deleteOutdatedOnboardings() {
		DateTime threshold = DateTime.now().minusDays(3);

		List<Screen> screens = new ScreenDAO().getAll();

		int deletedCount = 0;
		for (Screen screen : screens) {
			if (screen.getOwners().isEmpty() && threshold.isAfter(screen.getDateCreated().getTime())) {
				new ScreenDAO().deleteById(screen.getId());
				deletedCount += 1;
			}
		}

		if (deletedCount > 0) {
			logger.info(MessageFormat.format("Deleted {0} outdated onboarding requests from before {1}.", deletedCount,
					threshold));
		}
	}

}
