
API.prefix: dyniacData.session
dyniacData:
  session:
    mapper-localtion: com/example/demo/mapper/*.xml
    base-package: com.example.demo.dao1ss
    name: sessin_${server.port}
    namebean: $(test_s1)
    namebean2: '$(test_d2)'
    datasource: 
        name: test_d1
        url: jdbc:mysql://localhost:3306/test
        username: sky_
        password: sky5274
        driver-class-name: com.mysql.jdbc.Driver
    mysql:
      mapper-localtion: com/example/demo/mapper/*.xml
      base-package: com.example.*.dao
      datasource: 
        master:
          name: test_d2
          url: jdbc:mysql://localhost:3306/demo
          username: sky_
          password: sky5274
          driver-class-name: com.mysql.jdbc.Driver
        slaver:
          name: test_s1
          url: jdbc:mysql://localhost:3306/demo
          username: sky_
          password: sky5274
          driver-class-name: com.mysql.jdbc.Driver
          type: com.alibaba.druid.pool.DruidDataSource
    mysql2:
      mapper-localtion: com/example/demo/mapper/*.xml
      base-package: com.sky.m2.*.mapper
      datasource: 
          name: test_3
          url: jdbc:mysql://localhost:3306/test
          username: sky_
          password: sky5274
          driver-class-name: com.mysql.jdbc.Driver