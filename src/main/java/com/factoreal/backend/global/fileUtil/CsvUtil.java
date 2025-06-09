package com.factoreal.backend.global.fileUtil;

import com.factoreal.backend.domain.abnormalLog.application.AbnormalLogRepoService;
import com.factoreal.backend.domain.abnormalLog.dto.response.reportDetailResponse.*;
import com.factoreal.backend.domain.abnormalLog.entity.AbnormalLog;
import com.factoreal.backend.domain.controlLog.entity.ControlLog;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class CsvUtil {

    private final AbnormalLogRepoService abnormalLogRepoService;

    public CsvUtil(AbnormalLogRepoService abnormalLogRepoService) {
        this.abnormalLogRepoService = abnormalLogRepoService;
    }

    public Path writeReportAsCsv(PeriodDetailReportResponse rpt, Map<Long, ControlLog> ctlMap) throws IOException {
        Path tmp = Files.createTempFile("PreviewMonthReport-", ".csv");

        try (CSVWriter w = new CSVWriter(new FileWriter(tmp.toFile()))) {
            // 헤더에 targetName 추가
            w.writeNext(new String[]{"공간Id", "공간 이름", "이상치 유형",
                    "타겟 ID", "작업자/설비 이름", "발생 시간", "이상치 내용",
                    "위험 레벨", "이상치 값", "제어 타입", "제어 값", "점검일", "제어여부"
            });


            for (ZoneBlockResponse z : rpt.getZones()) {
                for (AbnDetailResponse d : z.getEnvAbnormals())
                    write(z, d, w, "ENV", d.getAbnormalId(), ctlMap, "-");

                for (WorkerBlockResponse wb : z.getWorkers())
                    for (AbnDetailResponse d : wb.getWorkerAbnormals())
                        write(z, d, w, "WORKER", d.getAbnormalId(), ctlMap, wb.getName());

                for (EquipBlockResponse eb : z.getEquips())
                    for (AbnDetailResponse d : eb.getFacAbnormals())
                        write(z, d, w, "EQUIP", d.getAbnormalId(), ctlMap, eb.getEquipId());
            }
        }

        return tmp;
    }

    private void write(
            ZoneBlockResponse z, AbnDetailResponse d, CSVWriter w, String tp,
            Long abnId, Map<Long, ControlLog> ctlMap, String targetName
    ) {
        ControlLog ctl = ctlMap.get(abnId);
        AbnormalLog abn = abnormalLogRepoService.findById(abnId);
        String targetId = abn.getTargetId();

        w.writeNext(new String[]{
                z.getZoneId(),
                z.getZoneName(),
                tp,
                targetId,
                targetName,
                d.getDetectedAt().toString(),
                String.valueOf(d.getAbnormalType()),
                String.valueOf(d.getDangerLevel()),
                String.valueOf(d.getAbnVal()),
                ctl != null ? ctl.getControlType() : "-",
                ctl != null ? String.valueOf(ctl.getControlVal()) : "-",
                ctl != null ? ctl.getExecutedAt().toString() : "-",
                ctl != null ? String.valueOf(ctl.getControlStat()) : "-"
        });
    }
}