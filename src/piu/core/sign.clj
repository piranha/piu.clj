(ns piu.core.sign
  (:import [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec]
           [java.security MessageDigest]
           [java.util Base64 Base64$Encoder Base64$Decoder])
  (:require [piu.config :as config]
            [mount.core :as mount]))


(set! *warn-on-reflection* true)


(defn ba   [^String s] (.getBytes s "UTF-8"))
(defn utf8 [^bytes ba] (String. ba "UTF-8"))


(defn ^SecretKeySpec KEY []
  (let [sha (MessageDigest/getInstance "SHA-1")
        ba  (->> (.digest sha (config/SECRET))
                 (take 16)
                 byte-array)]
    (SecretKeySpec. ba "AES")))


(def ^Base64$Encoder b64-encoder (.withoutPadding
                                   (Base64/getUrlEncoder)))
(def ^Base64$Decoder b64-decoder (Base64/getUrlDecoder))


(mount/defstate ^Cipher encrypter
  :start (doto (Cipher/getInstance "AES")
           (.init Cipher/ENCRYPT_MODE (KEY))))

(mount/defstate ^Cipher decrypter
  :start (doto (Cipher/getInstance "AES")
           (.init Cipher/DECRYPT_MODE (KEY))))


(defn encrypt [s]
  (->> (.doFinal encrypter (ba s))
       (.encodeToString b64-encoder)))


(defn decrypt [^String s]
  (try
    (->> (.decode b64-decoder s)
         (.doFinal decrypter)
         (utf8))
    (catch Exception _
      nil)))
