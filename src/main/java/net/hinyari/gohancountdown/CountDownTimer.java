package net.hinyari.gohancountdown;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CountDownTimer
{
    private GohanCountDown main;

    private long rawtime;
    private boolean isNTPLoad = false;
    private NTPUDPClient ntpClient;
    private TimeInfo timeInfo;
    private InetAddress ntpAddress;

    private ScheduledExecutorService service;

    public CountDownTimer(GohanCountDown main)
    {
        // メインクラスのインスタンス読み込み
        this.main = main;
        try {
            // NTP時刻合わせクライアントインスタンスを生成
            ntpClient = new NTPUDPClient();
            // NTPクライアントをopen
            ntpClient.open();
            // 接続先ホスト情報
            ntpAddress = InetAddress.getByName("ntp.nict.jp");
            // 時刻情報を取得する
            timeInfo = ntpClient.getTime(ntpAddress);
            rawtime = timeInfo.getReturnTime();
            
            // システム時間を取得する
            long systemtime = System.currentTimeMillis();
            // 誤差
            long errortime = rawtime - systemtime;
            main.getLabel_errortime().setText(String.valueOf(errortime) + "ms");
            log("systemtime " + systemtime);
            log("ntptime " + rawtime);
            log("errortime " + errortime);
            
            // NTPで取得した時間とシステム時間の差が1000ms以内だった場合
            // 誤差が1000ms以内であるのでシステム時間を使用する
            if (errortime < 1000 && errortime > -1000) {
                // NTPは必要ないと判断し、クローズする
                ntpClient.close();
                main.getLabel_server().setText("NTP時刻との誤差が小さいため");
                main.getLabel_appstatus().setText("システム時刻を使用します");
                // 特に意味のないような気がする
                isNTPLoad = false;
            } else {
                // NTP時間を利用する
                isNTPLoad = true;
            }
            // スレッドを立てる
            service = Executors.newSingleThreadScheduledExecutor();
            // 250ms間隔でスレッドを実行する
            service.scheduleAtFixedRate(this::reloadDisplay,
                    0, 500, TimeUnit.MILLISECONDS);
            
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

    public void reloadDisplay()
    {
        if (ntpClient == null) {
            main.getLabel_appstatus().setText("NTPクライアントが存在しません。");
            return;
        }
        
        // NTPクライアントが
        if (isNTPLoad) {
            if (!ntpClient.isOpen()) {
                main.getLabel_appstatus().setText("NTPサーバに接続出来ませんでした");
            }
            try {
                timeInfo = ntpClient.getTime(ntpAddress);
                rawtime = timeInfo.getReturnTime();
                main.getLabel_server().setText(ntpAddress.getHostName() + " / " + ntpAddress.getHostAddress());
                main.getLabel_appstatus().setText("正常");
            } catch (IOException e) {
                e.printStackTrace();
                main.getLabel_appstatus().setText("接続時にエラー");
            }
        } else {
            rawtime = System.currentTimeMillis();
        }
        
        log((isNTPLoad ? "NTP時刻 " : "システム時刻 " + rawtime));
        
        Instant nowTimeInstance = Instant.ofEpochMilli(rawtime);
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
            main.getLabel_unt2019h().setText(String.format("%02d", lunt2019seconds / 3600));
            main.getLabel_unt2019m().setText(String.format("%02d", (lunt2019seconds / 60) % 60));
            main.getLabel_unt2019s().setText(String.format("%02d", lunt2019seconds % 60));
            main.getLabel_from2018ss().setText(String.valueOf(durationfrom2018.getSeconds()));
        } else {    // 2019年後
            main.getLabel_unt2019h().setText("00");
            main.getLabel_unt2019m().setText("00");
            main.getLabel_unt2019s().setText("00");
            main.getLabel_yearsfrom().setText("2019年からの経過時間");
            main.getLabel_from2018ss().setText(String.valueOf(durationfrom2019.getSeconds()));
        }
        main.getLabel_unt2020ss().setText(String.valueOf(durationunt2020.getSeconds()));
    }

    public long getRawtime()
    {
        return rawtime;
    }
    
    private void log(Object msg)
    {
        System.out.println(msg);
    }

}
