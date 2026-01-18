import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { BuildControls } from "../BuildControls";

describe("BuildControls", () => {
  it("renders in select mode by default", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={null}
      />
    );

    expect(screen.getByText("Select Mode")).toBeInTheDocument();
    expect(screen.getByText("Build Wall Mode")).toBeInTheDocument();
    expect(screen.getByText("Stockpile")).toBeInTheDocument();
  });

  it("renders in build mode when buildMode is true", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={true}
        selectedCell={null}
      />
    );

    expect(screen.getByText("Select Mode")).toBeInTheDocument();
    expect(screen.getByText("Build Wall Mode")).toBeInTheDocument();
  });

  it("shows stockpile resource selector", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={null}
      />
    );

    expect(screen.getByText("Resource:")).toBeInTheDocument();
    expect(screen.getByText("Wood")).toBeInTheDocument();
    expect(screen.getByText("Food")).toBeInTheDocument();
  });

  it("shows stockpile capacity input", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={null}
      />
    );

    expect(screen.getByText("Capacity:")).toBeInTheDocument();
    const capacityInput = screen.getByRole("spinbutton");
    expect(capacityInput).toBeInTheDocument();
    expect(capacityInput).toHaveValue(100);
  });

  it("disables place stockpile button when no cell selected", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={null}
      />
    );

    const placeButton = screen.getByText("Place Stockpile");
    expect(placeButton).toBeDisabled();
  });

  it("enables place stockpile button when cell selected", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={[5, 10]}
      />
    );

    const placeButton = screen.getByText("Place Stockpile");
    expect(placeButton).not.toBeDisabled();
  });

  it("calls onToggleBuildMode when select mode button clicked", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={true}
        selectedCell={null}
      />
    );

    fireEvent.click(screen.getByText("Select Mode"));
    expect(onToggleBuildMode).toHaveBeenCalledTimes(1);
  });

  it("calls onToggleBuildMode when build mode button clicked", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={null}
      />
    );

    fireEvent.click(screen.getByText("Build Wall Mode"));
    expect(onToggleBuildMode).toHaveBeenCalledTimes(1);
  });

  it("calls onPlaceStockpile with correct args when place button clicked", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={[5, 10]}
      />
    );

    fireEvent.click(screen.getByText("Place Stockpile"));
    expect(onPlaceStockpile).toHaveBeenCalledWith([5, 10], "wood", 100);
  });

  it("allows changing stockpile resource", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={[5, 10]}
      />
    );

    const resourceSelector = screen.getByRole("combobox");
    
    fireEvent.change(resourceSelector, { target: { value: "food" } });
    
    fireEvent.click(screen.getByText("Place Stockpile"));
    expect(onPlaceStockpile).toHaveBeenCalledWith([5, 10], "food", 100);
  });

  it("allows changing stockpile capacity", () => {
    const onPlaceWallGhost = vi.fn();
    const onPlaceStockpile = vi.fn();
    const onToggleBuildMode = vi.fn();

    render(
      <BuildControls
        onPlaceWallGhost={onPlaceWallGhost}
        onPlaceStockpile={onPlaceStockpile}
        onToggleBuildMode={onToggleBuildMode}
        buildMode={false}
        selectedCell={[5, 10]}
      />
    );

    const capacityInput = screen.getByRole("spinbutton");
    
    fireEvent.change(capacityInput, { target: { value: "200" } });
    
    fireEvent.click(screen.getByText("Place Stockpile"));
    expect(onPlaceStockpile).toHaveBeenCalledWith([5, 10], "wood", 200);
  });
});
