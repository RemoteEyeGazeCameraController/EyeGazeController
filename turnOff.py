import time, mouse


def main():
    # minimize grid 3
    mouse.move(1430, 6)
    mouse.click()
    time.sleep(0.5)

    # open hardware settings
    mouse.move(55, 46)
    mouse.double_click()
    time.sleep(5)
    # go to eye tracker tab
    mouse.move(235, 192)
    mouse.click()
    time.sleep(1)
    # turn off switch
    mouse.move(645, 321)
    mouse.click()


if __name__ == "__main__":
    main()
