package com.fream.back.domain.accessLog.service.geo;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * IP를 이용하여 위치 정보(국가, 지역, 도시) 조회
 */
@Service
public class GeoIPService {

    private final DatabaseReader databaseReader;

    public GeoIPService() throws IOException {
        File database = new File(getClass().getClassLoader().getResource("GeoLite2-City.mmdb").getFile());
        this.databaseReader = new DatabaseReader.Builder(database).build();
    }

    public Location getLocation(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = databaseReader.city(ipAddress);

            Country country = response.getCountry();
            Subdivision subdivision = response.getMostSpecificSubdivision();
            City city = response.getCity();

            return new Location(
                    country.getName(),
                    subdivision.getName(),
                    city.getName()
            );
        } catch (Exception e) {
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
