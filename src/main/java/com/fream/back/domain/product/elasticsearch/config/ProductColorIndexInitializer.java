package com.fream.back.domain.product.elasticsearch.config;

import com.fream.back.domain.product.elasticsearch.index.ProductColorIndex;
import com.fream.back.domain.product.elasticsearch.service.ProductColorIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(2)
public class ProductColorIndexInitializer implements CommandLineRunner {

    private final ProductColorIndexingService indexingService;
    private final ElasticsearchOperations esOperations;
//    @PostConstruct
//    public void initIndex() {
//        indexingService.indexAllColors();
//    }
@Override
public void run(String... args) {
    IndexOperations idxOps = esOperations.indexOps(ProductColorIndex.class);

    // 1) 인덱스 존재 시 삭제(개발용)
    if (idxOps.exists()) {
        idxOps.delete();
    }




    // 2) Settings (동의어)
    Document settings = Document.parse("""
        {
            "analysis": {
              "tokenizer": {
                "nori_tokenizer_custom": {
                  "type": "nori_tokenizer",
                  "decompound_mode": "discard",
                  "user_dictionary": "analysis/userdict_ko.txt"
                }
              },
              "filter": {
                "my_synonym_filter": {
                  "type": "synonym_graph",
                  "synonyms_path": "analysis/synonyms.txt",
                  "updateable": true
                },
                "my_nori_edge_ngram": {
                  "type": "edge_ngram",
                  "min_gram": 2,
                  "max_gram": 4
                }
              },
              "analyzer": {
                "my_nori_base_analyzer": {
                  "type": "custom",
                  "tokenizer": "nori_tokenizer_custom",
                  "filter": [
                    "lowercase",
                    "my_nori_edge_ngram"
                  ]
                },
                "my_nori_synonym_analyzer": {
                  "type": "custom",
                  "tokenizer": "nori_tokenizer_custom",
                  "filter": [
                    "lowercase",
                    "my_synonym_filter",
                    "my_nori_edge_ngram"
                  ]
                }
              }
            }
        }
        """);

    // 3) Mapping
    Document mapping = Document.parse("""
        {
            "properties": {
              "productName": {
                "type": "text",
                "analyzer": "my_nori_base_analyzer",
                "search_analyzer": "my_nori_synonym_analyzer"
              },
              "brandName": {
                "type": "text",
                "analyzer": "my_nori_base_analyzer",
                "search_analyzer": "my_nori_synonym_analyzer"
              },
              "categoryName": {
                "type": "text",
                "analyzer": "my_nori_base_analyzer",
                "search_analyzer": "my_nori_synonym_analyzer"
              },
              "collectionName": {
                "type": "text",
                "analyzer": "my_nori_base_analyzer",
                "search_analyzer": "my_nori_synonym_analyzer"
              },
              "colorName": {
                "type": "text",
                "analyzer": "my_nori_base_analyzer",
                "search_analyzer": "my_nori_synonym_analyzer"
              }
            }
        }
        """);


    // 2) Settings 문서 - 동의어 없이, Nori만 사용
//    Document settings = Document.parse("""
//            {
//                "analysis": {
//                  "tokenizer": {
//                    "nori_tokenizer_custom": {
//                      "type": "nori_tokenizer",
//                      "decompound_mode": "mixed",
//                      "user_dictionary": "analysis/userdict_ko.txt"
//                    }
//                  },
//                  "filter": {
//                     "my_nori_edge_ngram": {
//                     "type": "edge_ngram",
//                     "min_gram": 1,
//                      "max_gram": 4
//                    }
//                  },
//                  "analyzer": {
//                    "my_nori_analyzer": {
//                      "type": "custom",
//                      "tokenizer": "nori_tokenizer_custom",
//                      "filter": [
//                        "lowercase",
//                        "my_nori_edge_ngram"
//                      ]
//                    }
//                  }
//                }
//            }
//            """);

    // 3) 매핑(Mappings) 문서 - analyzer / search_analyzer 모두 Nori만 사용
//    Document mapping = Document.parse("""
//            {
//                "properties": {
//                  "productName": {
//                    "type": "text",
//                    "analyzer": "my_nori_analyzer",
//                    "search_analyzer": "my_nori_analyzer"
//                  },
//                  "brandName": {
//                    "type": "text",
//                    "analyzer": "my_nori_analyzer",
//                    "search_analyzer": "my_nori_analyzer"
//                  },
//                  "categoryName": {
//                    "type": "text",
//                    "analyzer": "my_nori_analyzer",
//                    "search_analyzer": "my_nori_analyzer"
//                  },
//                  "collectionName": {
//                    "type": "text",
//                    "analyzer": "my_nori_analyzer",
//                    "search_analyzer": "my_nori_analyzer"
//                  },
//                  "colorName": {
//                    "type": "text",
//                    "analyzer": "my_nori_analyzer",
//                    "search_analyzer": "my_nori_analyzer"
//                  }
//                }
//            }
//            """);

    // 4) 인덱스 생성 with settings
    idxOps.create(settings);

    // 5) 매핑 적용
    idxOps.putMapping(mapping);

    // 6) 데이터 인덱싱
    indexingService.indexAllColors();
}
}
