# sns-clj

Embedded sns for clojure

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.bigsy/sns-clj.svg)](https://clojars.org/org.clojars.bigsy/sns-clj)
### Development:

```clojure
(require 'sns-clj.core)

;; Start a local sns with default port:
(init-sns)

;; another call will halt the previous system:
(init-sns)

;; When you're done:
(halt-sns!)
```

### Testing:

**NOTE**: these will halt running sns instances

```clojure
(require 'clojure.test)

(use-fixtures :once with-sns-fn)

(defn around-all
  [f]
  (with-sns-fn "optional file path for db.json"
                    f))

(use-fixtures :once around-all)


; You can also wrap ad-hoc code in init/halt:
(with-sns "optional file path for db.json"
  do-something) 
  ```
