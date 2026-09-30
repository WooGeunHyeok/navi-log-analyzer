-- =====================================================================
-- Docker(MariaDB 컨테이너) 최초 기동 시 자동 실행되는 초기화 스크립트
-- (/docker-entrypoint-initdb.d 에 마운트됨. DB 볼륨이 비어있을 때 딱 한 번만 실행)
--
-- DB 생성 / 사용자 생성은 docker-compose.yml 의 MARIADB_* 환경변수가 처리하므로
-- 여기서는 Batch(MyBatis)가 먼저 사용하는 TBL_LOG_DICTIONARY 테이블만 만든다.
-- 나머지 테이블은 Backend(JPA ddl-auto=update)가 자동 생성한다.
-- =====================================================================

CREATE TABLE IF NOT EXISTS TBL_LOG_DICTIONARY (
    ID              BIGINT       NOT NULL AUTO_INCREMENT,
    FILE_NAME       VARCHAR(255) NOT NULL,
    FILE_PATH       VARCHAR(500) NOT NULL,
    FUNCTION_NAME   VARCHAR(255) NOT NULL,
    LINE_NUMBER     INT          NOT NULL,
    LOG_TYPE        VARCHAR(50)  NOT NULL,
    RAW_MESSAGE     TEXT,
    PATTERN_MESSAGE TEXT,
    CALLS           TEXT,
    STATUS          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    INSDATE         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UPDDATE         DATETIME(6)  NULL,
    PRIMARY KEY (ID),
    UNIQUE KEY UK_LOG_DICTIONARY_LOCATION (FILE_PATH, FUNCTION_NAME, LINE_NUMBER)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
