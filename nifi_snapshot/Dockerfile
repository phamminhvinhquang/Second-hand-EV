# Dùng bản NiFi mới nhất làm nền
FROM apache/nifi:latest

# [QUAN TRỌNG]: XÓA hoặc COMMENT dòng copy nifi.properties đi.
# Để NiFi tự động sinh cấu hình SSL (keystore/truststore) khi khởi động.
# COPY --chown=nifi:nifi nifi.properties /opt/nifi/nifi-current/conf/nifi.properties

# Chỉ copy file Flow để giữ lại luồng dữ liệu cũ
COPY --chown=nifi:nifi flow.json.gz /opt/nifi/nifi-current/conf/flow.json.gz