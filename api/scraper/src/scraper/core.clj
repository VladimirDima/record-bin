(ns scraper.core
  (:require [uswitch.lambada.core :refer [deflambdafn]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-http.client :as http]
            [hickory.core :refer [as-hickory parse]]
            [hickory.select :as s]))

(def media-type-regexes
  [#"(soundcloud)\.com\/[^/]+?(?:\/sets)?\/[^/]+$"
   #"open\.(spotify)\.com\/(?:track|album|user\/[^/]+\/playlist)\/[^/]+$"
   #".+?\.(bandcamp)\.com\/(?:album|track)\/[^/]+$"
   #"(youtube)\.com\/(?:watch)\?v=.+?$"])

(defn url-decode [encoded-url]
  (if encoded-url (java.net.URLDecoder/decode encoded-url)))

(defn url-encode [decoded-url]
  (if decoded-url (java.net.URLEncoder/encode decoded-url)))

(defn construct-request-url [media-type media-url]
  (case media-type
    :bandcamp media-url
    :soundcloud "https://soundcloud.com/oembed"
    :spotify
    (str
      "https://embed.spotify.com/oembed?url="
      (url-encode media-url))
    :youtube
    (str
      "https://www.youtube.com/oembed?url="
      (url-encode media-url)
      "&format=json")))

(defn get-media-url [event]
  (url-decode (get-in event ["queryStringParameters" "url"])))

(defn get-media-type [media-url]
  (some
    #(if-let [[full domain & others]  %] (keyword domain))
    (map #(re-find % media-url) media-type-regexes)))

(defn parse-oembed [response]
  (-> response
      :body
      json/read-str
      ((fn [body]
         {:title (get body "title")
          :thumbnailUrl (get body "thumbnail_url")
          :embedUrl (last (re-find #"src=\"(.+?)\"" (get body "html")))}))))

(defn plaintext [s] (-> s (string/replace #"\s+" " ") string/trim))

(defn get-text-content
  ([node] (apply str (reduce get-text-content [] (node :content))))
  ([coll node] (conj coll (if (string? node) node (get-text-content node)))))

(defn scrape-bandcamp [request-url]
  (let [site-htree (-> (http/get request-url) :body parse as-hickory)]
    {:title
     (-> (s/select (s/id :name-section) site-htree)
         first get-text-content plaintext)
     :thumbnailUrl
     (-> (s/select (s/child (s/id :tralbumArt) (s/tag :a) (s/tag :img)) site-htree)
         first :attrs :src)
     :embedUrl
     (-> (filter
           #(string/includes? % "EmbeddedPlayer")
           (map #(get-in % [:attrs :content]) (s/select (s/tag :meta) site-htree)))
         first)}))

(defn scrape-soundcloud [request-url media-url]
  (parse-oembed
    (http/post
      request-url
      {:headers {"Content-Type" "application/json"}
       :body (json/write-str {:url media-url :format "json"})})))

(defn scrape-spotify [request-url]
  (parse-oembed (http/get request-url)))

(defn scrape-youtube [request-url]
  (parse-oembed (http/get request-url)))

(defn scrape [media-type media-url]
  (let [request-url (construct-request-url media-type media-url)] 
    (assoc
      (case media-type
        :bandcamp (scrape-bandcamp request-url)
        :soundcloud (scrape-soundcloud request-url media-url)
        :spotify (scrape-spotify request-url)
        :youtube (scrape-youtube request-url))
      :url media-url :mediaType (.toUpperCase (name media-type)))))

(defn construct-response [status body]
  {:statusCode status
   :body (json/write-str body)
   :headers {"Content-Type" "application/json"
             "Access-Control-Allow-Origin" "*"}
   :isBase64Encoded false})

(defn handle [event]
  (if-let [media-url (get-media-url event)]
    (if-let [media-type (get-media-type media-url)]
      (construct-response 200 (scrape media-type media-url))
      (construct-response 400 {:error "Invalid URL."}))
    (construct-response 400 {:error "No URL supplied."})))

(deflambdafn scraper.core.RecordBinScraper 
  [in out context]
  (let [event (json/read (io/reader in))
        result (handle event)]
    (with-open [w (io/writer out)]
      (json/write result w))))
