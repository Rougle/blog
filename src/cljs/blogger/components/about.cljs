(ns blogger.components.about)

;; TODO Content here should have similar editor to blog entries
(defn about []
  (fn []
    [:div
     [:h1 "This blog"]
     [:p "This is my personal blog. I plan to mostly write about web-development and related subjects. Amateur game
     development and music production might also be valid topics."]
     [:h2 "Me"]
     [:p "I'm a software developer with university background. I have mostly worked in various areas
     of web development. In addition to slacking I spend my freetime on music and
     occasional game development with Godot - although the latter has been on hiatus and hasn't really resulted
     in anything worthwhile.

     I play blues, funk and rock with guitar. In addition to one garage band and occasional jams with friends I
     do bedroom music production time to time. I'm by no means a professional musician or producer, but
     if you happen to need a soundtrack for a game or mod I would be happy to participate."]]))