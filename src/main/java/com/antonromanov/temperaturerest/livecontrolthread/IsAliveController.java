package com.antonromanov.temperaturerest.livecontrolthread;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.stereotype.Service;
import com.antonromanov.temperaturerest.model.Logs;
import com.antonromanov.temperaturerest.model.Status;
import com.antonromanov.temperaturerest.service.MainService;

import java.sql.Time;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static com.antonromanov.temperaturerest.utils.Utils.*;

/**
 * Поток, мониторящий состояние датчиков по таймаутам.
 */
@Service
public class IsAliveController implements Runnable {

    /**
     * Инжектим наш сервис.
     */
    MainService mainService;

    /**
     * Конструктор.
     *
     * @param mainService
     */
    public IsAliveController(MainService mainService) {
        this.mainService = mainService;
    }

    /**
     * Собсна, основной метод, где вся логика.
     */
    @Override
    public void run() {

        System.out.println("Thread will started....");
        boolean ret = true;

        if (mainService != null) { // проверяем, чтобы сервис вообще доступен был

            while (ret) { // бесконечный цикл.
                try {

               //     System.out.println("Тестим Чек-таймаут - " + checkTimeout(mainService.getMainParametrs().getLastPingTime()));
                    Date date = new Date();
                    Time time = new Time(date.getTime()); // берем текущее время.

                    /**
                     * Печатаем всякую муру. TODO ее потом логгировать бы в логи гласфиша.
                     */
                //    System.out.println("Параметры MainParametrs. AC - " + mainService.getMainParametrs().isAcStatus());
              //      System.out.println("Параметры MainParametrs. LAN - " + mainService.getMainParametrs().isLanStatus());
               //     System.out.println("is logged?  " + mainService.getMainParametrs().isLogged());

                    /**
                     *  Первое условие:
                     *
                     *  + Таймаут просрочен (false)
                     *  + Не логгировали это раньше
                     *  + АС или LAN по последним логам включен.
                     */
                    if (!checkTimeout(mainService.getMainParametrs().getLastPingTime()) && (!mainService.getMainParametrs().isLogged()) &&
                            (mainService.getMainParametrs().isAcStatus() || mainService.getMainParametrs().isLanStatus())) { // Хьюстон, у нас проблемы.....

                 //       System.out.println("Хьюстон, у нас проблемы.....");

                        /**
                         * Пишет в БД запись, что связи нет, чтобы ее фронтенд прочитать мог.
                         */
                        Status log = new Status("REST: NO PING", false, false, 0, 0, time, time,
                                0, 0, 0, 0, new Date());

                        try {
                            List<Logs> tempLogs = mainService.addLog(log); // пишем через сервис.
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        mainService.getMainParametrs().setLogged(true); // теперь мы залоггировали трабл
                        mainService.getMainParametrs().setAcStatus(false); // ставим флаг АС - нет
                        mainService.getMainParametrs().setLanStatus(false);// ставим флаг LAN - нет
                        mainService.getMainParametrs().setJustStartedSituation(false); // ну и это теперь не "только стартанули2 ситуация


                        /**
                         * Следующая ситуация - проблем или решлась или ее не было. Условия:
                         *
                         * + АС - включено
                         * + LAN - включен
                         */
                    } else if ((mainService.getMainParametrs().isAcStatus() || mainService.getMainParametrs().isLanStatus())) {

                  //      System.out.println("Проблема решена или ее и не было");
                        mainService.getMainParametrs().setLogged(false); // убираем, что мы залогировались

                        /**
                         * Стартовая ситуация:
                         *
                         * + Таймаут по пингу просрочен
                         * + незалогированы
                         * + статусы сети и 220 отрицательные
                         */
                    } else if (!checkTimeout(mainService.getMainParametrs().getLastPingTime()) && !mainService.getMainParametrs().isLogged() &&
                            (!mainService.getMainParametrs().isAcStatus() || !mainService.getMainParametrs().isLanStatus())
                            ) { // стартовая ситуация
                 //       System.out.println("Стартовая ситуация");
                    }

                    /**
                     * ждем 10 секунд.
                     */
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } catch (BeanCreationException exception) {
            //        System.out.println("Косяк");
                }
            }
        }
    }

}
