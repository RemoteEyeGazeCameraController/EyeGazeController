import time, mouse


def main():
    # go to eye tracker tab
    mouse.move(235, 192)
    mouse.click()
    time.sleep(0.75)
    # turn off switch
    mouse.move(645, 321)
    mouse.click()
    time.sleep(0.5)

    # Close hardware settings
    mouse.move(1767, 34)
    mouse.click()
    time.sleep(0.5)

    # open Grid3
    mouse.move(170, 740)
    mouse.click()


if __name__ == "__main__":
    main()
