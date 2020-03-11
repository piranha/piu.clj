(ns piu.core.sign
  (:import [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec]
           [java.security MessageDigest]
           [java.util Base64 Base64$Encoder Base64$Decoder]))


(set! *warn-on-reflection* true)


(defn b [^String s]
  (.getBytes s "UTF-8"))


(def SECRET (or (System/getenv "SECRET")
                (binding [*out* *err*]
                  (print "\nWARNING: set 'SECRET' env variable to be secure\n\n")
                  "epic-secret")))


(def ^SecretKeySpec KEY
  (let [sha (MessageDigest/getInstance "SHA-1")
        ba  (->> (.digest sha (b SECRET))
                 (take 16)
                 byte-array)]
    (SecretKeySpec. ba "AES")))


(def ^Base64$Encoder b64-encoder (.withoutPadding
                                   (Base64/getUrlEncoder)))
(def ^Base64$Decoder b64-decoder (Base64/getUrlDecoder))


(def ^Cipher encrypter (doto (Cipher/getInstance "AES")
                         (.init Cipher/ENCRYPT_MODE KEY)))
(def ^Cipher decrypter (doto (Cipher/getInstance "AES")
                         (.init Cipher/DECRYPT_MODE KEY)))


(defn encrypt [s]
  (->> (.doFinal encrypter (b s))
       (.encodeToString b64-encoder)))


(defn -decrypt [^String s]
  (String.
    (->> (.decode b64-decoder s)
         (.doFinal decrypter))
    "UTF-8"))


(defn decrypt [s]
  (try
    (-decrypt s)
    (catch Exception e
      nil)))
