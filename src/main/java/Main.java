import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

public class Main {

	private static final String PROD_ID = "-//HAN//iCal4j 1.0//EN";
	private static final String ALARM_DESCRIPTION_TEMPLATE = "{} 복습";
	private static final String EVENT_SUMMARY_TEMPLATE = "{} 복습 일정: D+";
	private static final String ALARM_TRIGGER_DURATION = "-PT1H"; // 1시간 전 알림
	private static final List<Integer> REVIEW_INTERVALS = Arrays.asList(1, 7, 14, 28, 56, 84, 168, 365);

	public static void main(String[] args) throws Exception {
		String subject = getSubjectFromUser();
		LocalDate startDate = LocalDate.now();

		Calendar calendar = buildSpacedRepetitionCalendar(startDate, REVIEW_INTERVALS, subject);
		writeCalendarToFile(calendar, subject + ".ics");
	}

	private static String getSubjectFromUser() {
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.print("복습할 과목/주제를 입력하세요: ");
			return scanner.nextLine().trim();
		}
	}

	private static Calendar buildSpacedRepetitionCalendar(LocalDate startDate, List<Integer> intervals,
		String subject) throws Exception {
		Calendar calendar = createCalendar();

		for (int daysAfterStart : intervals) {
			LocalDate reviewDate = startDate.plusDays(daysAfterStart);
			VEvent event = createReviewEvent(reviewDate, daysAfterStart, subject);
			calendar.getComponents().add(event);
		}

		return calendar;
	}

	private static Calendar createCalendar() {
		Calendar calendar = new Calendar();
		calendar.getProperties().add(new ProdId(PROD_ID));

		Version version = new Version();
		version.setValue("2.0");
		calendar.getProperties().add(version);

		calendar.getProperties().add(new CalScale("GREGORIAN"));
		return calendar;
	}

	private static VEvent createReviewEvent(LocalDate reviewDate, int dayCount, String subject) {
		VEvent event = new VEvent();

		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.set(java.util.Calendar.YEAR, reviewDate.getYear());
		cal.set(java.util.Calendar.MONTH, reviewDate.getMonthValue() - 1);
		cal.set(java.util.Calendar.DAY_OF_MONTH, reviewDate.getDayOfMonth());
		cal.set(java.util.Calendar.HOUR_OF_DAY, 18);
		cal.set(java.util.Calendar.MINUTE, 10);
		cal.set(java.util.Calendar.SECOND, 0);

		net.fortuna.ical4j.model.DateTime startDateTime = new net.fortuna.ical4j.model.DateTime(cal.getTime());
		event.getProperties().add(new DtStart(startDateTime));

		String summary = EVENT_SUMMARY_TEMPLATE.replace("{}", subject) + dayCount;
		event.getProperties().add(new Summary(summary));

		event.getProperties().add(new Uid(UUID.randomUUID().toString()));

		event.getAlarms().add(createAlarm(subject));

		return event;
	}

	private static VAlarm createAlarm(String subject) {
		VAlarm alarm = new VAlarm();
		alarm.getProperties().add(new Trigger(new Dur(ALARM_TRIGGER_DURATION)));
		alarm.getProperties().add(new Action("DISPLAY"));
		alarm.getProperties().add(new Description(ALARM_DESCRIPTION_TEMPLATE.replace("{}", subject)));
		return alarm;
	}

	private static void writeCalendarToFile(Calendar calendar, String fileName) throws Exception {
		String directoryName = "iCal";
		File dir = new File(directoryName);
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("디렉토리 생성 실패: " + directoryName);
		}

		String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
		String fullPath = String.format("%s/%s-%s", directoryName, currentDate, fileName);

		try (FileOutputStream fos = new FileOutputStream(fullPath)) {
			new CalendarOutputter().output(calendar, fos);
			System.out.println(fullPath + " 파일 생성 완료!");
		}
	}
}