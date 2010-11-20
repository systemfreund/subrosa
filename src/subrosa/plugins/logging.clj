(ns subrosa.plugins.logging
  (:use [subrosa.hooks :only [add-hook]]
        [subrosa.client]
        [subrosa.config :only [config]])
  (:import [java.util Date]
           [java.text SimpleDateFormat]
           [java.io File FileWriter]))

(def time-formatter (SimpleDateFormat. "HH:mm:ss"))
(def date-formatter (SimpleDateFormat. "yyyy-MM-dd"))

(defn get-log-name [log-dir room-name]
  (str log-dir "/" room-name "_" (.format date-formatter (Date.)) ".log"))

(defn append [room-name msg]
  (let [log-dir (File. (System/getProperty "user.dir")
                       (config :logging :directory))]
    (.mkdirs log-dir)
    (with-open [out (FileWriter. (get-log-name log-dir room-name) true)]
      (.write out (.toCharArray
                   (str (.format time-formatter (Date.)) " " msg "\n"))))))

(defmulti log-dispatch (fn [& args] (first args)))

(defmethod log-dispatch 'privmsg-room-hook [hook channel room-name msg]
  (append room-name (format "<%s> %s" (nick-for-channel channel) msg)))

(defmethod log-dispatch 'join-hook [hook channel room-name]
  (append room-name
          (format "--- join: %s (%s) joined %s"
                  (nick-for-channel channel)
                  (format-client channel)
                  room-name)))

(defmethod log-dispatch 'part-hook [hook channel room-name]
  (append room-name
          (format "--- part: %s left %s"
                  (nick-for-channel channel)
                  room-name)))

(defmethod log-dispatch 'quit-hook [hook channel quit-msg]
  (doseq [room-name (rooms-for-nick (nick-for-channel channel))]
    (append room-name
            (format "--- quit: %s (Quit: %s)"
                    (nick-for-channel channel)
                    (or quit-msg "Client Quit")
                    room-name))))

(defn log [& args]
  (when (config :logging :directory)
    (apply log-dispatch args)))

(add-hook 'privmsg-room-hook log)
(add-hook 'join-hook log)
(add-hook 'part-hook log)
(add-hook 'quit-hook log)

