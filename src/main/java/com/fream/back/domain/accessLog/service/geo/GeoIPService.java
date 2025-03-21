package com.fream.back.domain.accessLog.service.geo;

import com.fream.back.domain.accessLog.exception.AccessLogErrorCode;
import com.fream.back.domain.accessLog.exception.GeoIPException;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP를 이용하여 위치 정보(국가, 지역, 도시) 조회
 */
@Service
@Slf4j
public class GeoIPService {

    private final DatabaseReader databaseReader;

    /**
     * GeoIP 데이터베이스를 초기화합니다.
     *
     * @throws GeoIPException GeoIP 데이터베이스 초기화 실패 시
     */
    public GeoIPService() {
        try {
            // (1) ClassPathResource로 mmdb 파일을 가져옴
            ClassPathResource resource = new ClassPathResource("GeoLite2-City.mmdb");

            // (2) InputStream 으로 DatabaseReader 생성
            try (InputStream inputStream = resource.getInputStream()) {
                this.databaseReader = new DatabaseReader.Builder(inputStream).build();
            }
        } catch (IOException e) {
            log.error("GeoIP 데이터베이스 초기화 실패", e);
            throw new GeoIPException(AccessLogErrorCode.GEO_IP_DATABASE_ERROR,
                    "GeoIP 데이터베이스를 초기화하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * IP 주소로부터 위치 정보를 조회합니다.
     *
     * @param ip IP 주소
     * @return 위치 정보 (국가, 지역, 도시)
     */
    public Location getLocation(String ip) {
        if (ip == null || ip.isEmpty() || "localhost".equals(ip) || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            // 로컬 주소이거나 IP가 없는 경우
            return new Location("Unknown", "Unknown", "Unknown");
        }

        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = databaseReader.city(ipAddress);

            Country country = response.getCountry();
            Subdivision subdivision = response.getMostSpecificSubdivision();
            City city = response.getCity();

            return new Location(
                    country != null ? country.getName() : "Unknown",
                    subdivision != null ? subdivision.getName() : "Unknown",
                    city != null ? city.getName() : "Unknown"
            );
        } catch (UnknownHostException e) {
            log.warn("잘못된 IP 주소 형식: {}", ip, e);
            throw new GeoIPException(AccessLogErrorCode.INVALID_IP_ADDRESS,
                    "잘못된 IP 주소 형식입니다: " + ip, e);
        } catch (Exception e) {
            log.error("IP 위치정보 조회 중 오류 발생: {}", ip, e);
            // 위치 정보 조회 실패시 기본값 반환 (서비스 중단 방지)
            return new Location("Unknown", "Unknown", "Unknown");
        }
    }

    public static class Location {
        private final String country;
        private final String region;
        private final String city;

        public Location(String country, String region, String city) {
            this.country = country;
            this.region = region;
            this.city = city;
        }

        public String getCountry() {
            return country;
        }
        public String getRegion() {
            return region;
        }
        public String getCity() {
            return city;
        }
    }
}