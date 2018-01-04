package net.hinyari.gohancountdown;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CountDownTimer {
	private GohanCountDown main;

	private long rawTime;
	private boolean isNTPLoad = false;
	private NTPUDPClient ntpClient;
	private TimeInfo timeInfo;
	private final String ntpAddress = "http://ntp-a1.nict.go.jp/cgi-bin/jst";

	private ScheduledExecutorService service;

	public CountDownTimer(GohanCountDown main) {
		// メインクラスのインスタンス読み込み
		this.main = main;
		try {
			// NTP時刻を取得する
			long ntptime = getNTPTime();

			// システム時間を取得する
			long systemtime = System.currentTimeMillis();
			// 誤差
			long errortime = ntptime - systemtime;
			main.getLabel_errortime().setText(String.valueOf(errortime) + "ms");
			log("systemtime " + systemtime);
			log("ntptime " + ntptime);
			log("errortime " + errortime);

			// NTPで取得した時間とシステム時間の差が1000ms以内だった場合
			// 誤差が1000ms以内であるのでシステム時間を使用する
			if (errortime < 1000 && errortime > -1000) {
				main.getLabel_server().setText("NTP時刻との誤差が小さいため");
				main.getLabel_appstatus().setText("システム時刻を使用します");
				// 特に意味のないような気がする
				isNTPLoad = false;
			} else {
				// NTP時間を利用する
				isNTPLoad = true;
			}
			log("isNTPLoad " + isNTPLoad);
			// スレッドを立てる
			service = Executors.newSingleThreadScheduledExecutor();
			// 250ms間隔でスレッドを実行する
			service.scheduleAtFixedRate(this::reloadDisplay, 0, 500, TimeUnit.MILLISECONDS);

		} catch (UnknownHostException e) {
			main.getLabel_server().setText("不明なホスト名");
			e.printStackTrace();
			ntpClient.close();
		} catch (IOException e) {
			main.getLabel_server().setText("接続時にエラー");
			e.printStackTrace();
			ntpClient.close();
		}
	}

	private int time = 60;

	private void reloadDisplay() {
		// NTPクライアントが
		if (isNTPLoad) {
			try {
				// 60回目（30秒目の試行）
				if (time % 60 == 0) {
					main.getLabel_appstatus().setText("取得中");
					rawTime = getNTPTime();
					time = time - 60;
					long errortime = rawTime - System.currentTimeMillis();
					main.getLabel_server().setText(ntpAddress);
					log("int time " + time);

					// NTPで取得した時間とシステム時間の差が1000ms以内だった場合
					// 誤差が1000ms以内であるのでシステム時間を使用する
					if (errortime < 1000 && errortime > -1000) {
						main.getLabel_server().setText("NTP時刻との誤差が小さいため");
						main.getLabel_appstatus().setText("システム時刻を使用します");
						isNTPLoad = false;
					}
				} else {
					main.getLabel_appstatus().setText("時刻を" + divide60(time) + "秒後に再取得します…");
					if (time % 2 == 0)
						rawTime = rawTime + 1000;
					log("int time " + time);
				}
			} catch (IOException e) {
				e.printStackTrace();
				main.getLabel_appstatus().setText("接続時にエラー");
			}
			time++;
		} else {
			rawTime = System.currentTimeMillis();
		}

		log(((isNTPLoad ? "最終NTP時刻 " : "システム時刻 ") + rawTime));

		Instant nowTimeInstance = Instant.ofEpochMilli(rawTime);
		// 現在時刻を埋め込む
		LocalDateTime ldt = LocalDateTime.ofInstant(nowTimeInstance, ZoneId.systemDefault());
		main.getLabel_nowtime().setText(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(ldt));

		// LocalDateTimeを取得する
		Instant firstDayof2018 = LocalDateTime.parse("2018-01-01T00:00:00", DateTimeFormatter.ISO_DATE_TIME)
				.toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.EPOCH));
		Instant firstDayof2019 = LocalDateTime.parse("2019-01-01T00:00:00", DateTimeFormatter.ISO_DATE_TIME)
				.toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.EPOCH));
		Instant firstDayof2020 = LocalDateTime.parse("2020-01-01T00:00:00", DateTimeFormatter.ISO_DATE_TIME)
				.toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.EPOCH));

		// 時刻の差をそれぞれ取得する
		Duration durationunt2019 = Duration.between(nowTimeInstance, firstDayof2019);
		Duration durationfrom2019 = Duration.between(firstDayof2019, nowTimeInstance);
		Duration durationfrom2018 = Duration.between(firstDayof2018, nowTimeInstance);
		Duration durationunt2020 = Duration.between(nowTimeInstance, firstDayof2020);
		long lunt2019seconds = durationunt2019.getSeconds();

		// JLabelに文字を置く
		main.getLabel_unt2019ss().setText(lunt2019seconds <= 0 ? "HAPPY NEW YEAR" : String.valueOf(lunt2019seconds));

		// 2018年内
		if (lunt2019seconds >= 0) {
			main.getLabel_untdays().setText(String.format("%03d", lunt2019seconds / 86400));
			main.getLabel_unt2019h()
					.setText(String.format("%02d", lunt2019seconds / 3600 - ((lunt2019seconds / 86400 * 24))));
			main.getLabel_unt2019m().setText(String.format("%02d", (lunt2019seconds / 60) % 60));
			main.getLabel_unt2019s().setText(String.format("%02d", lunt2019seconds % 60));
			main.getLabel_from2018ss().setText(String.valueOf(durationfrom2018.getSeconds()));
		} else { // 2019年後
			main.getLabel_untdays().setText("000");
			main.getLabel_unt2019h().setText("00");
			main.getLabel_unt2019m().setText("00");
			main.getLabel_unt2019s().setText("00");
			main.getLabel_yearsfrom().setText("2019年からの経過時間");
			main.getLabel_from2018ss().setText(String.valueOf(durationfrom2019.getSeconds()));
		}
		main.getLabel_unt2020ss().setText(String.valueOf(durationunt2020.getSeconds()));
	}

	private String divide60(int i) {
		// 0回目の試行だった場合30秒後を返す
		if (i == 0 || i == 1)
			return "30";

		// 2で割り切れない奇数
		if (i % 2 == 1) {
			i--;
		}

		i = i / 2;

		log("divide 60 " + i);

		return String.valueOf(30 - i);
	}

	private void log(Object msg) {
		Logger logger = Logger.getLogger(GohanCountDown.class.getName());
		logger.info(String.valueOf(msg));
	}

	private long getNTPTime() throws IOException {
		Document document = Jsoup.connect(ntpAddress).get();
		Elements elements = document.body().getAllElements();
		StringBuilder sb = new StringBuilder();
		for (Element element : elements) {
			if (element.ownText() == null) {
				continue;
			}
			sb.append(element.ownText()).append("\n");
		}

		String rawdata = sb.toString().replace(".", "").replace("\n", "");

		return Long.valueOf(rawdata);
	}

}
